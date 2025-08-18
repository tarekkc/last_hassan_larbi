package com.yourcompany.clientmanagement.view;

import com.yourcompany.clientmanagement.model.Versment;
import com.yourcompany.clientmanagement.model.Client;
import com.yourcompany.clientmanagement.controller.ClientController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class VersmentDialog extends JDialog {
    private JComboBox<ClientItem> clientComboBox;
    private JTextField montantField;
    private JComboBox<String> typeComboBox;
    private JTextField datePaiementField;
    private JTextField anneeConcerneeField;
    private boolean confirmed = false;
    private Versment versment;
    private ClientController clientController;
    private List<ClientItem> allClientItems;
    private DefaultComboBoxModel<ClientItem> clientModel;

    // Helper class for client combo box
    private static class ClientItem {
        private final Client client;

        public ClientItem(Client client) {
            this.client = client;
        }

        public Client getClient() {
            return client;
        }

        @Override
        public String toString() {
            return client.getNom() + " " + (client.getPrenom() != null ? client.getPrenom() : "") + 
                   " (" + client.getId() + ")";
        }
    }

    public VersmentDialog(JFrame parent, String title, Versment versment) {
        super(parent, title, true);
        this.versment = versment;
        this.clientController = new ClientController();
        initializeUI();
        populateFields();
    }

    private void initializeUI() {
        setSize(800, 400);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        // Main form panel
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Client selection with search
        JPanel clientSearchPanel = new JPanel(new BorderLayout());
        clientModel = new DefaultComboBoxModel<>();
        clientComboBox = new JComboBox<>(clientModel);
        clientComboBox.setEditable(true);

        // Get the editor component
        JTextField searchField = (JTextField) clientComboBox.getEditor().getEditorComponent();
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String searchText = searchField.getText().toLowerCase();
                    if (e.getKeyCode() != KeyEvent.VK_ENTER && 
                        e.getKeyCode() != KeyEvent.VK_UP && 
                        e.getKeyCode() != KeyEvent.VK_DOWN) {
                        filterClients(searchText);
                    }
                });
            }
        });

        // Handle selection from dropdown
        clientComboBox.addActionListener(e -> {
            if (!clientComboBox.isPopupVisible() && clientComboBox.getSelectedItem() instanceof ClientItem) {
                ClientItem selected = (ClientItem) clientComboBox.getSelectedItem();
                searchField.setText(selected.toString());
            }
        });

        clientSearchPanel.add(new JLabel("Client*:"), BorderLayout.WEST);
        clientSearchPanel.add(clientComboBox, BorderLayout.CENTER);
        formPanel.add(clientSearchPanel);
        formPanel.add(new JLabel()); // Empty cell for grid layout

        loadClients();

        // Amount field
        addFormField(formPanel, "Montant*:", montantField = new JTextField());
        addNumericValidation(montantField);

        // Type field
        addFormField(formPanel, "Type*:", typeComboBox = new JComboBox<>());
        typeComboBox.addItem("Honoraires");
        typeComboBox.addItem("Avance");
        typeComboBox.addItem("Remboursement");
        typeComboBox.addItem("Autre");

        // Payment date field
        addFormField(formPanel, "Date Paiement*:", datePaiementField = new JTextField());
        datePaiementField.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        datePaiementField.setToolTipText("Format: YYYY-MM-DD");

        // Year concerned field
        addFormField(formPanel, "Année Concernée*:", anneeConcerneeField = new JTextField());
        anneeConcerneeField.setText(String.valueOf(LocalDate.now().getYear()));

        JScrollPane scrollPane = new JScrollPane(formPanel);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        JButton okButton = new JButton("Enregistrer");
        okButton.addActionListener(e -> validateAndClose());
        okButton.setPreferredSize(new Dimension(120, 30));

        JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(e -> dispose());
        cancelButton.setPreferredSize(new Dimension(120, 30));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Set default button for Enter key
        getRootPane().setDefaultButton(okButton);
    }

    private void filterClients(String searchText) {
        clientModel.removeAllElements();
        
        if (searchText.isEmpty()) {
            // Show all clients when search is empty
            for (ClientItem item : allClientItems) {
                clientModel.addElement(item);
            }
        } else {
            // Filter clients based on search text
            List<ClientItem> filtered = allClientItems.stream()
                .filter(item -> item.toString().toLowerCase().contains(searchText))
                .collect(Collectors.toList());
            
            for (ClientItem item : filtered) {
                clientModel.addElement(item);
            }
        }
        
        clientComboBox.setPopupVisible(true);
        ((JTextField)clientComboBox.getEditor().getEditorComponent()).setText(searchText);
    }

    private void addFormField(JPanel panel, String label, JComponent field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void addNumericValidation(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!(Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE || c == '.')) {
                    e.consume();
                }
            }
        });
    }

    private void loadClients() {
        List<Client> clients = clientController.fetchAllClients();
        allClientItems = clients.stream()
            .map(ClientItem::new)
            .collect(Collectors.toList());
        
        for (ClientItem item : allClientItems) {
            clientModel.addElement(item);
        }
    }

    private void populateFields() {
        if (versment == null) return;

        // Select the correct client
        for (int i = 0; i < clientComboBox.getItemCount(); i++) {
            ClientItem item = clientComboBox.getItemAt(i);
            if (item.getClient().getId() == versment.getClientId()) {
                clientComboBox.setSelectedIndex(i);
                break;
            }
        }

        montantField.setText(versment.getMontant() != null ? versment.getMontant().toString() : "");
        typeComboBox.setSelectedItem(versment.getType());
        
        if (versment.getDatePaiement() != null) {
            datePaiementField.setText(versment.getDatePaiement().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        
        anneeConcerneeField.setText(versment.getAnneeConcernee());
    }

    private void validateAndClose() {
        // Validate required fields
        Object selected = clientComboBox.getSelectedItem();
        if (selected == null || (selected instanceof String && ((String)selected).isEmpty())) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client valide", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!(selected instanceof ClientItem)) {
            // Try to find matching client
            String searchText = selected.toString();
            for (ClientItem item : allClientItems) {
                if (item.toString().equalsIgnoreCase(searchText)) {
                    clientComboBox.setSelectedItem(item);
                    selected = item;
                    break;
                }
            }
            
            if (!(selected instanceof ClientItem)) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client valide", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (montantField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le montant est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (typeComboBox.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Le type est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (datePaiementField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La date de paiement est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (anneeConcerneeField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "L'année concernée est obligatoire", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate amount format
        try {
            new BigDecimal(montantField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Format de montant invalide", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate date format
        try {
            LocalDate.parse(datePaiementField.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Format de date invalide (YYYY-MM-DD)", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Versment getVersment() {
        if (!confirmed) return null;

        Versment v = versment != null ? versment : new Versment();
        
        Object selected = clientComboBox.getSelectedItem();
        if (!(selected instanceof ClientItem)) {
            return null;
        }
        
        ClientItem selectedClient = (ClientItem) selected;
        v.setClientId(selectedClient.getClient().getId());
        v.setMontant(new BigDecimal(montantField.getText().trim()));
        v.setType((String) typeComboBox.getSelectedItem());
        v.setDatePaiement(LocalDate.parse(datePaiementField.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        v.setAnneeConcernee(anneeConcerneeField.getText().trim());
        
        // Set creation time for new versments
        if (versment == null) {
            v.setCreatedAt(LocalDateTime.now());
        }

        return v;
    }
}
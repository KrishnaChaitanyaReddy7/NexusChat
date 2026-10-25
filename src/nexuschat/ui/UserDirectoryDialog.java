package nexuschat.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Unit V Syllabus:
 * - Swing Components: JDialog, JTable, JScrollPane, JButton
 * - Layout managers: BorderLayout, FlowLayout
 * - Displaying structured tabular records
 */
public class UserDirectoryDialog extends JDialog {

    private final JTable userTable;
    private final DefaultTableModel tableModel;

    public UserDirectoryDialog(Frame parent, String[] userDataArray) {
        super(parent, "NexusChat - User Directory", true);
        setSize(550, 350);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(8, 8));

        // Column names for JTable
        String[] columns = {"Username", "Full Name", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make cells read-only
            }
        };

        userTable = new JTable(tableModel);
        userTable.setRowHeight(24);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Populate table from user array
        if (userDataArray != null) {
            for (String entry : userDataArray) {
                String[] parts = entry.split("::");
                if (parts.length >= 3) {
                    tableModel.addRow(new Object[]{parts[0], parts[1], parts[2]});
                } else if (parts.length >= 1) {
                    tableModel.addRow(new Object[]{parts[0], parts[0], "OFFLINE"});
                }
            }
        }

        // Add table inside a JScrollPane
        JScrollPane scrollPane = new JScrollPane(userTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with FlowLayout
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel countLabel = new JLabel("Total Registered Users: " + tableModel.getRowCount());
        JButton closeButton = new JButton("Close");

        // Unit IV: Event Handling with ActionListener
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        bottomPanel.add(countLabel);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}

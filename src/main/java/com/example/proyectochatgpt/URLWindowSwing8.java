package com.example.proyectochatgpt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Timer;


public class URLWindowSwing8 {
    private boolean cleanModeActive = false;
    private Timer cleanModeTimer = new Timer();

    private boolean stop = false;
    private long minInterval = 10000;
    private long maxInterval = 60000;
    private List<String> urls = new ArrayList<>();
    private int[] urlCounts;
    private Random random = new Random();
    private DefaultListModel<String> urlListModel = new DefaultListModel<>();
    private JList<String> urlList = new JList<>(urlListModel);
    private JCheckBox ghostModeCheckBox = new JCheckBox("GhostMode");
    private JCheckBox cleanModeCheckBox = new JCheckBox("CleanMode");

    private int incognitoWindowCount = 0;
    private int maxIncognitoWindows = 5;
    private List<Window> incognitoWindows = new ArrayList<>(); // Almacenar ventanas incógnitas
    private long cleanModeTime = 60000; // Valor predeterminado de 1 minuto en milisegundos
    private int totalWindowCount = 0; // Para rastrear todas las ventanas, no solo las de incógnito
    private Set<Process> openedChromeProcesses = new HashSet<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            URLWindowSwing8 urlWindow = new URLWindowSwing8();
            urlWindow.createAndShowGUI();
        });
    }

    void createAndShowGUI() {
        JFrame frame = new JFrame("URL Repetidor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 400);
        frame.setLayout(new BorderLayout());

        JButton executeButton = createStyledButton("Ejecutar", Color.orange, Color.black);
        JButton stopButton = createStyledButton("Detener", Color.red, Color.white);
        JButton intervalButton = createStyledButton("Modificar Intervalo", Color.blue, Color.white);
        JButton urlsButton = createStyledButton("Introducir Enlaces", Color.green, Color.black);
        JButton maxIncognitoButton = createStyledButton("Ventanas Killer", Color.yellow, Color.black);

        maxIncognitoButton.addActionListener(e -> {
            String inputMaxIncognito = JOptionPane.showInputDialog(null, "Ingrese la cantidad de ventanas máximas A cerrar:",
                    "Configuración de Ventanas", JOptionPane.PLAIN_MESSAGE);

            try {
                int newMaxIncognitoWindows = Integer.parseInt(inputMaxIncognito);
                if (newMaxIncognitoWindows >= 0) {
                    maxIncognitoWindows = newMaxIncognitoWindows;
                } else {
                    showError("La cantidad máxima debe ser un número no negativo.");
                    return;
                }
            } catch (NumberFormatException ex) {
                showError("Por favor, ingrese un número válido para la cantidad máxima de ventanas");
                return;
            }

            String inputCleanModeTime = JOptionPane.showInputDialog(null, "Ingrese el TIEMPO en minutos antes de cerrar las ventanas:",
                    "Configuración de CleanMode", JOptionPane.PLAIN_MESSAGE);

            try {
                long cleanModeTimeMinutes = Long.parseLong(inputCleanModeTime);
                if (cleanModeTimeMinutes >= 1) {
                    // Convertir minutos a milisegundos
                    cleanModeTime = cleanModeTimeMinutes * 60000;
                } else {
                    showError("El tiempo mínimo debe ser de al menos 1 minuto.");
                }
            } catch (NumberFormatException ex) {
                showError("Por favor, ingrese un número válido para el tiempo antes de cerrar las ventanas.");
            }
        });


        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(executeButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(intervalButton);
        buttonPanel.add(urlsButton);
        buttonPanel.add(ghostModeCheckBox);
        buttonPanel.add(cleanModeCheckBox);
        buttonPanel.add(maxIncognitoButton);

        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(urlList), BorderLayout.CENTER);

        executeButton.addActionListener(e -> {
            if (urls.isEmpty()) {
                showError("No hay enlaces introducidos");
                return;
            }

            stop = false;
            totalWindowCount = 0; // Reiniciar el contador de todas las ventanas

            new Thread(() -> {
                while (!stop) {
                    try {
                        int index = random.nextInt(urls.size());
                        String url = urls.get(index);
                        try {
                            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "start", "chrome", "--new-window");

                            if (ghostModeCheckBox.isSelected()) {
                                processBuilder.command().add("--incognito");
                            }

                            processBuilder.command().add(url);

                            Process process = processBuilder.start();
                            totalWindowCount++; // Incrementar el contador de todas las ventanas

                            if (totalWindowCount >= maxIncognitoWindows && cleanModeCheckBox.isSelected()) {
                                // Cerrar todas las ventanas después de alcanzar la cantidad máxima
                                cleanModeActive = true;
                                cleanModeTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        try {
                                            // Cerrar todas las ventanas después del tiempo especificado
                                            ProcessBuilder taskkillBuilder = new ProcessBuilder("cmd", "/c", "taskkill", "/IM", "chrome.exe", "/F");
                                            Process taskkillProcess = taskkillBuilder.start();
                                            taskkillProcess.waitFor();
                                            totalWindowCount = 0; // Reiniciar el contador después de cerrar las ventanas
                                            cleanModeActive = false;
                                        } catch (IOException | InterruptedException ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }, cleanModeTime); // Usar cleanModeTime en lugar de un valor fijo

                                while (cleanModeActive) {
                                    // Esperar hasta que se complete CleanMode
                                    Thread.sleep(1000); // Pausa de 1 segundo
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        urlCounts[index]++;
                        updateUrlList();
                        long interval = minInterval + (long) (random.nextDouble() * (maxInterval - minInterval));
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();
        });


        stopButton.addActionListener(e -> {
            stop = true;
            closeAllIncognitoWindows();
        });

        intervalButton.addActionListener(e -> showIntervalDialog());

        urlsButton.addActionListener(e -> showUrlsDialog());

        frame.setVisible(true);
    }

    private JButton createStyledButton(String text, Color backgroundColor, Color textColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 18));
        button.setBackground(backgroundColor);
        button.setForeground(textColor);
        button.setFocusPainted(false); // Evita el resaltado del borde al hacer clic
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(button.getBackground().darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(backgroundColor);
            }
        });
        return button;
    }

    private void showIntervalDialog() {
        JDialog intervalDialog = new JDialog();
        intervalDialog.setTitle("Modificar Intervalo");
        intervalDialog.setSize(300, 150);
        intervalDialog.setLayout(new GridLayout(3, 2));

        JLabel minLabel = new JLabel("Intervalo Mínimo (ms):");
        JTextField minIntervalField = new JTextField(Long.toString(minInterval));
        JLabel maxLabel = new JLabel("Intervalo Máximo (ms):");
        JTextField maxIntervalField = new JTextField(Long.toString(maxInterval));
        JButton okButton = new JButton("OK");

        okButton.addActionListener(e -> {
            try {
                minInterval = Long.parseLong(minIntervalField.getText());
                maxInterval = Long.parseLong(maxIntervalField.getText());
                if (minInterval > maxInterval) {
                    showError("El intervalo mínimo no puede ser mayor que el máximo");
                    return;
                }
                intervalDialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Los intervalos deben ser números válidos");
            }
        });

        intervalDialog.add(minLabel);
        intervalDialog.add(minIntervalField);
        intervalDialog.add(maxLabel);
        intervalDialog.add(maxIntervalField);
        intervalDialog.add(okButton);

        intervalDialog.setVisible(true);
    }

    private void showUrlsDialog() {
        JDialog urlsDialog = new JDialog();
        urlsDialog.setTitle("Introducir Enlaces");
        urlsDialog.setSize(250, 100);
        urlsDialog.setLayout(new GridLayout(2, 2));

        JLabel countLabel = new JLabel("Número de enlaces:");
        JTextField urlCountField = new JTextField();
        JButton okButton = new JButton("OK");

        okButton.addActionListener(e -> {
            try {
                int urlCount = Integer.parseInt(urlCountField.getText());
                urlsDialog.dispose();

                urls.clear();
                urlCounts = new int[urlCount];
                for (int i = 0; i < urlCount; i++) {
                    showUrlDialog(i + 1);
                }
                updateUrlList();
            } catch (NumberFormatException ex) {
                showError("El número de enlaces debe ser un número válido");
            }
        });

        urlsDialog.add(countLabel);
        urlsDialog.add(urlCountField);
        urlsDialog.add(okButton);

        urlsDialog.setVisible(true);
    }

    private void showUrlDialog(int index) {
        JDialog urlDialog = new JDialog();
        urlDialog.setTitle("Introducir Enlace " + index);
        urlDialog.setSize(350, 150);
        urlDialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridLayout(2, 1));

        JLabel urlLabel = new JLabel("Enlace " + index + ":");
        JTextField urlField = new JTextField();
        contentPanel.add(urlLabel);
        contentPanel.add(urlField);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = createStyledButton("OK", Color.blue, Color.white);
        okButton.addActionListener(e -> {
            urls.add(urlField.getText());
            urlDialog.dispose();
        });
        JButton cancelButton = createStyledButton("Cancelar", Color.red, Color.white);
        cancelButton.addActionListener(e -> urlDialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        urlDialog.add(contentPanel, BorderLayout.CENTER);
        urlDialog.add(buttonPanel, BorderLayout.SOUTH);

        urlDialog.setVisible(true);
    }

    private void updateUrlList() {
        SwingUtilities.invokeLater(() -> {
            urlListModel.clear();
            for (int i = 0; i < urls.size(); i++) {
                urlListModel.addElement(urls.get(i) + " (" + urlCounts[i] + ")");
            }
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void openIncognitoWindow(String url) {
        JFrame incognitoFrame = new JFrame("Incognito Window");
        incognitoFrame.setSize(800, 600);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(editorPane);
        incognitoFrame.add(scrollPane);

        try {
            editorPane.setPage(url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        incognitoFrame.setVisible(true);
        incognitoWindows.add(incognitoFrame);
        incognitoWindowCount++;

        // Cerrar ventanas incógnitas si se supera el límite
        if (incognitoWindowCount > maxIncognitoWindows) {
            closeExcessIncognitoWindows();
        }
    }

    private void closeExcessIncognitoWindows() {
        int excessCount = incognitoWindowCount - maxIncognitoWindows;
        if (excessCount > 0) {
            for (int i = 0; i < excessCount; i++) {
                if (!incognitoWindows.isEmpty()) {
                    Window window = incognitoWindows.remove(0);
                    window.dispose(); // Cerrar la ventana incógnita
                    incognitoWindowCount--;
                }
            }
        }
    }

    private void closeAllIncognitoWindows() {
        for (Window window : incognitoWindows) {
            window.dispose(); // Cerrar todas las ventanas incógnitas
        }
        incognitoWindows.clear();
        incognitoWindowCount = 0;
    }
}


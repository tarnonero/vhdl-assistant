import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

import static javax.swing.SwingUtilities.invokeLater;

@SuppressWarnings("unchecked")
public class VHDL extends JPanel implements ListSelectionListener {
    private static final String[] LABELS = {"MODEL COMPONENT", "MODEL ENTITY", "MODEL SIGNALS"};
    private static final String addString = "ADD";
    private static final String removeString = "REMOVE";
    private static final String printString = "PRINT";
    private static String fileName = "";
    private JList<String> selectedList, lastSelectedList;
    private DefaultListModel<String> listModel;
    private DefaultListModel<String> componentModel = new DefaultListModel<>();
    private Map<String, JList<String>> labelMap = new HashMap<>();
    private JButton removeButton;
    private JTextField textField;
    private Entity entity = new Entity();
    private ArrayList<String> signal = new ArrayList<>();
    private ArrayList<Component> component = new ArrayList<>();
    private ArrayList<String> instance = new ArrayList<>();
    private StringBuilder buffer = new StringBuilder();
    private String componentName;
    private int counter = 0;    //instance counter
    private Map<String, DefaultListModel<String>> components = new HashMap<>();


    private VHDL() {

        setLayout(new GridLayout(1, 2));
        String[] elements = {};
        JList<String> list;

        for (String label : LABELS) {
            listModel = new DefaultListModel<>();
            for (String s : elements) {
                listModel.addElement(s);
            }

            list = new JList<>(listModel);
            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            list.setVisibleRowCount(-1);
            list.getSelectionModel().addListSelectionListener(this);
            list.addMouseListener(new ClickListener());
            list.setName(label);
            labelMap.put(label, list);

            JScrollPane scrollPane = new JScrollPane(list);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

            JPanel container = new JPanel(new BorderLayout());
            container.add(scrollPane);
            container.setBorder(BorderFactory.createTitledBorder(label));

            add(container);
        }

        JButton addButton = new JButton(addString);
        AddListener addListener = new AddListener(addButton);
        addButton.setActionCommand(addString);
        addButton.addActionListener(addListener);
        addButton.setEnabled(false);

        removeButton = new JButton(removeString);
        removeButton.setActionCommand(removeString);
        removeButton.addActionListener(new RemoveListener());

        JButton instanceButton = new JButton("INSTANTIATE");
        instanceButton.setActionCommand("INSTANTIATE");
        instanceButton.addActionListener(new InstanceListener(instanceButton));

        JButton printButton = new JButton(printString);
        printButton.setActionCommand(printString);
        printButton.addActionListener(new PrintListener(printButton));

        JPanel buttonContainer = new JPanel(new GridLayout(4, 0));
        buttonContainer.add(addButton);
        buttonContainer.add(removeButton);
        buttonContainer.add(instanceButton);
        buttonContainer.add(printButton);

        JPanel textFieldPanel = new JPanel(new FlowLayout());
        textField = new JTextField(28);
        textField.addActionListener(addListener);
        textField.getDocument().addDocumentListener(addListener);
        textFieldPanel.add(textField);

        JPanel userInput = new JPanel(new GridLayout(2, 0));
        userInput.add(textFieldPanel);
        userInput.add(buttonContainer);

        add(userInput);
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("VHDL Assistant");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new VHDL());
        frame.setSize(1250, 200);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        invokeLater(VHDL::createAndShowGui);
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        } else {
            if (selectedList.getSelectedIndex() == -1) {
                removeButton.setEnabled(false);
            } else {
                removeButton.setEnabled(true);
            }

        }
    }

    private void clearFile() {
        try {
            PrintStream writer = new PrintStream(new FileOutputStream(fileName));
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void printInstance() {
        try {

            PrintStream writer = new PrintStream(new FileOutputStream(fileName, true));
            clearFile();
            System.setOut(writer);

            System.out.println("library ieee;");
            System.out.println("use ieee.std_logic_1164.all;" + "\n");

            System.out.println("entity " + entity.getName() + "is");
            System.out.println("    port(");
            System.out.println("    " + entity.getPort().getInput() + ";");
            System.out.println("    " + entity.getPort().getOutput() + ");");
            System.out.println("end " + entity.getName() + ";\n");

            System.out.println("architecture structural of " + entity.getName() + "is");

            for (Component c : component) {
                System.out.println("    component " + c.getName());
                System.out.println("        port(");
                System.out.println("        " + c.getPort().getInput() + ";");
                System.out.println("        " + c.getPort().getOutput() + ");");
                System.out.println("    end component;" + "\n");
            }

            for (Component c : component) {
                System.out.println("    for all: " + c.getName() + "use entity work." + c.getName() + "(behav);");
            }
            System.out.println();

            System.out.println("    signal ");
            for (String x : signal) {
                System.out.println("     " + x + ";");
            }
            System.out.println();

            System.out.println("    begin");
            for (String str : instance) {
                System.out.println("        " + str);
            }
            System.out.println("end structural;");


            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateList(JList<String> list) {
        lastSelectedList = selectedList;
        selectedList = list;
        listModel = (DefaultListModel<String>) list.getModel();
        lastSelectedList.setBorder(new LineBorder(Color.white));
        selectedList.setBorder(new LineBorder(Color.BLACK, 3));
    }

    private void setEntity() {
        String[] parts = new String[3];
        Port port = new Port();

        int nameIndex = selectedList.getSelectedValue().indexOf("port");
        parts[0] = selectedList.getSelectedValue().substring(0, nameIndex);
        int beginIndex = selectedList.getSelectedValue().indexOf("(") + 1;
        int endIndex = selectedList.getSelectedValue().indexOf(")");

        String portSeq = selectedList.getSelectedValue().substring(beginIndex, endIndex);
        String[] tmp = portSeq.split(";");

        parts[1] = tmp[0];
        parts[2] = tmp[1];

        port.setInput(parts[1]);
        port.setOutput(parts[2]);

        entity.setName(parts[0]);
        entity.setPort(port);
    }

    private void setComponent() {
        List<String> values = selectedList.getSelectedValuesList();
        Port port = new Port();
        Component c = new Component();

        for (String s : values) {

            String[] parts = new String[3];

            int nameIndex = s.indexOf("port");
            parts[0] = s.substring(0, nameIndex);
            int beginIndex = s.indexOf("(") + 1;
            int endIndex = s.indexOf(")");

            String portSeq = s.substring(beginIndex, endIndex);
            String[] tmp = portSeq.split(";");

            parts[1] = tmp[0];
            parts[2] = tmp[1];

            port.setInput(parts[1]);
            port.setOutput(parts[2]);

            c.setName(parts[0]);
            c.setPort(port);

            component.add(c);
        }
    }

    private void createInstance() {
        JList compWires;
        JList compNames;

        for (Component c : component) {
            String cName;
            DefaultListModel<String> compPort = new DefaultListModel<>();
            //Component's name
            cName = c.getName();
            String input = c.getPort().getInput();
            String output = c.getPort().getOutput();

            String iType = input.substring(input.indexOf(":") + 2, input.indexOf(":") + 4);
            String iDataType = input.substring(input.indexOf(":") + 5);
            String oType = output.substring(output.indexOf(":") + 2, output.indexOf(":") + 5);
            String oDataType = output.substring(output.indexOf(":") + 6);

            if (!input.contains(",")) {
                compPort.addElement(input);
            } else {
                ArrayList<String> inputs = new ArrayList<>(Arrays.asList(input.split(",")));
                for (int i = 0; i < inputs.size() - 1; i++) {
                    String s = inputs.get(i);
                    StringBuilder aux = new StringBuilder();

                    if (s.charAt(0) == ' ') {
                        s = s.substring(1);
                    }
                    aux.append(s).append(": ").append(iType).append(" ").append(iDataType);
                    s = aux.toString();
                    compPort.addElement(s);
                    aux.setLength(0);
                }
                int index = inputs.size() - 1;
                String s = inputs.get(index);
                if (s.charAt(0) == ' ')
                    s = s.substring(1);

                compPort.addElement(s);
            }

            if (!output.contains(",")) {
                compPort.addElement(output);
            } else {
                ArrayList<String> outputs = new ArrayList<>(Arrays.asList(output.split(",")));
                for (int i = 0; i < outputs.size() - 1; i++) {
                    String s = outputs.get(i);
                    if (!s.contains(oType)) {
                        if (s.charAt(0) == ' ')
                            s = s.substring(1);

                        StringBuilder aux = new StringBuilder();
                        aux.append(s).append(": ").append(oType).append(" ").append(oDataType);
                        s = aux.toString();
                        compPort.addElement(s);
                        aux.setLength(0);
                    }
                    int index = outputs.size() - 1;
                    String tmp = outputs.get(index);
                    if (tmp.charAt(0) == ' ')
                        tmp = tmp.substring(1);
                    compPort.addElement(tmp);
                }
            }
            //adds component's name + its signals list
            components.put(cName, compPort);
        }
        DefaultListModel<String> cNames = new DefaultListModel<>();
        for (Map.Entry<String, DefaultListModel<String>> kv : components.entrySet()) {
            cNames.addElement(kv.getKey());
        }
        compNames = new JList(cNames);
        compNames.setName("COMPNAMES");
        compNames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compNames.setVisibleRowCount(-1);
        compNames.getSelectionModel().addListSelectionListener(this);
        compNames.addMouseListener(new ClickListener());

        JScrollPane compNamesScroller = new JScrollPane(compNames);
        compNamesScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel compNamesContainer = new JPanel(new BorderLayout());
        compNamesContainer.add(compNamesScroller);
        compNamesContainer.setBorder(BorderFactory.createTitledBorder("COMPONENT NAMES"));

        compWires = new JList(componentModel);
        compWires.setName("COMPWIRES");
        compWires.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compWires.setVisibleRowCount(-1);
        compWires.getSelectionModel().addListSelectionListener(this);
        compWires.addMouseListener(new ClickListener());

        JScrollPane compWiresScroller = new JScrollPane(compWires);
        compWiresScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel componentWiresContainer = new JPanel(new BorderLayout());
        componentWiresContainer.add(compWiresScroller);
        componentWiresContainer.setBorder(BorderFactory.createTitledBorder("COMPONENT SIGNALS"));

        //entity list
        JList entWires;
        DefaultListModel<String> entPort = new DefaultListModel<>();

        String input = entity.getPort().getInput();
        String output = entity.getPort().getOutput();

        String iType = input.substring(input.indexOf(":") + 2, input.indexOf(":") + 4);
        String iDataType = input.substring(input.indexOf(":") + 5);
        String oType = output.substring(output.indexOf(":") + 2, output.indexOf(":") + 5);
        String oDataType = output.substring(output.indexOf(":") + 6);

        if (!input.contains(",")) {
            entPort.addElement(input);
        } else {
            ArrayList<String> inputs = new ArrayList<>(Arrays.asList(input.split(",")));
            for (int i = 0; i < inputs.size() - 1; i++) {
                String s = inputs.get(i);
                StringBuilder aux = new StringBuilder();

                if (s.charAt(0) == ' ') {
                    s = s.substring(1);
                }
                aux.append(s).append(": ").append(iType).append(" ").append(iDataType);
                s = aux.toString();
                entPort.addElement(s);
                aux.setLength(0);
            }
            int index = inputs.size() - 1;
            String s = inputs.get(index);
            if (s.charAt(0) == ' ')
                s = s.substring(1);
            entPort.addElement(s);
        }

        if (!output.contains(",")) {
            entPort.addElement(output);
        } else {
            ArrayList<String> outputs = new ArrayList<>(Arrays.asList(output.split(",")));
            for (int i = 0; i < outputs.size() - 1; i++) {
                String s = outputs.get(i);
                if (!s.contains(oType)) {
                    if (s.charAt(0) == ' ')
                        s = s.substring(1);

                    StringBuilder aux = new StringBuilder();
                    aux.append(s).append(": ").append(oType).append(" ").append(oDataType);
                    s = aux.toString();
                    entPort.addElement(s);
                    aux.setLength(0);
                }
                int index = outputs.size() - 1;
                String tmp = outputs.get(index);
                if (tmp.charAt(0) == ' ')
                    tmp = tmp.substring(1);
                entPort.addElement(tmp);
            }
        }

        entWires = new JList(entPort);
        entWires.setName("ENTWIRES");
        entWires.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        entWires.setVisibleRowCount(-1);
        entWires.getSelectionModel().addListSelectionListener(this);
        entWires.addMouseListener(new ClickListener());

        JScrollPane entityWiresScroller = new JScrollPane(entWires);
        entityWiresScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel entityContainer = new JPanel(new BorderLayout());
        entityContainer.add(entityWiresScroller);
        entityContainer.setBorder(BorderFactory.createTitledBorder("ENTITY SIGNALS"));

        DefaultListModel<String> signals = new DefaultListModel<>();
        for (String x : signal) {
            signals.addElement(x);
        }
        JList signalsList = new JList(signals);
        signalsList.setName("SIGNALS");
        signalsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        signalsList.setVisibleRowCount(-1);
        signalsList.getSelectionModel().addListSelectionListener(this);
        signalsList.addMouseListener(new ClickListener());

        JScrollPane signalsScroller = new JScrollPane(signalsList);
        signalsScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel signalsContainer = new JPanel(new BorderLayout());
        signalsContainer.add(signalsScroller);
        signalsContainer.setBorder(BorderFactory.createTitledBorder("SIGNALS"));

        JPanel listContainer = new JPanel(new GridLayout());
        listContainer.add(compNamesContainer);
        listContainer.add(componentWiresContainer);
        listContainer.add(signalsContainer);
        listContainer.add(entityContainer);

        JButton bindingButton = new JButton("BIND");
        bindingButton.setActionCommand("BIND");
        bindingButton.addActionListener(new BindingListener());

        JButton addInstanceButton = new JButton("ADD INSTANCE");
        addInstanceButton.setActionCommand("ADD INSTANCE");
        addInstanceButton.addActionListener(new AddInstanceListener());

        JPanel buttonContainer = new JPanel(new GridLayout(2, 0));
        buttonContainer.add(bindingButton);
        buttonContainer.add(addInstanceButton);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(listContainer);
        mainContainer.add(buttonContainer, BorderLayout.EAST);

        JFrame frame = new JFrame();
        frame.setSize(1200, 200);
        frame.setTitle("Bindings");
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.add(mainContainer);
    }

    class ClickListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
                if (e.getSource() != null) {
                    JList<String> list = (JList<String>) e.getSource();

                    for (Map.Entry<String, JList<String>> val : labelMap.entrySet()) {
                        if (val.getKey().equals(list.getName())) {
                            updateList(val.getValue());

                            switch (list.getName()) {
                                case "MODEL ENTITY":
                                    setEntity();
                                    break;
                                case "MODEL COMPONENT":
                                    setComponent();
                                    break;
                                case "MODEL SIGNALS":
                                    List<String> values = selectedList.getSelectedValuesList();
                                    for (String s : values) {
                                        if (!(signal.contains(s)))
                                            signal.add(s);
                                    }
                                    break;
                                default:
                                    throw new IllegalStateException("Unexpected value: " + list.getName());
                            }
                        }
                    }

                    // * char as regex
                    if (list.getName().equals("COMPWIRES")) {
                        updateList(list);
                        buffer.append(selectedList.getSelectedValue()).append("*");
                    }
                    if (list.getName().equals("ENTWIRES")) {
                        updateList(list);
                        buffer.append(selectedList.getSelectedValue()).append("*");
                    }
                    if (list.getName().equals("SIGNALS")) {
                        updateList(list);
                        buffer.append(selectedList.getSelectedValue()).append("*");
                    }
                    if (list.getName().equals("COMPNAMES")) {
                        updateList(list);
                        componentName = selectedList.getSelectedValue();

                        componentModel.removeAllElements();

                        for (Map.Entry<String, DefaultListModel<String>> kv : components.entrySet()) {
                            if (kv.getKey().equals(componentName)) {
                                Object[] tmp = kv.getValue().toArray();
                                for (Object o : tmp) {
                                    if (!componentModel.contains(o)) {
                                        componentModel.addElement(o.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {


        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    class AddInstanceListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            StringBuilder tmp = new StringBuilder();
            String label = entity.getName().charAt(0) + "" + counter;

            int endIndex = buffer.length() - 1;
            String portMap = buffer.substring(0, endIndex);

            label = label.concat(": ");
            tmp.append(label).append(componentName).append(" ").append("port map(").append(portMap).append(");");

            instance.add(tmp.toString());

            //incrementing counter and resetting buffer && componentName
            counter++;
            buffer.setLength(0);
            componentName = "";
        }
    }

    class InstanceListener implements ActionListener {
        JButton button;

        InstanceListener(JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            createInstance();
        }
    }


    class BindingListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String tmp = buffer.toString();
            String str1, str2;
            if (tmp.contains("*")) {
                str1 = tmp.substring(0, tmp.indexOf("*"));
                int index = tmp.indexOf("*") + 1;
                str2 = tmp.substring(index);
                str1 = str1.substring(0, str1.indexOf(":"));
                str2 = str2.substring(0, str2.indexOf(":"));

                StringBuilder aux = new StringBuilder();
                aux.append(str1).append(" => ").append(str2).append(",");
                buffer = aux;
            }
        }
    }

    class PrintListener implements ActionListener {
        JButton button;

        PrintListener(JButton button) {
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.showSaveDialog(null);
            fileName = fileChooser.getSelectedFile().toString();

            Set<Component> set = new HashSet<>(component);
            component.clear();
            component.addAll(set);
            printInstance();
        }
    }

    class AddListener implements ActionListener, DocumentListener {
        private boolean alreadyEnabled = false;
        private JButton button;

        AddListener(JButton button) {
            this.button = button;
        }

        //Required by ActionListener.
        public void actionPerformed(ActionEvent e) {
            String name = textField.getText();


            if (name.equals("") || alreadyInList(name)) {
                Toolkit.getDefaultToolkit().beep();
                textField.requestFocusInWindow();
                textField.selectAll();
                return;
            }

            int index = selectedList.getSelectedIndex();
            if (index == -1) { //no selection, so insert at beginning
                index = 0;
            } else {           //add after the selected item
                index++;
            }

            listModel.insertElementAt(name, index);
            //Reset the text field.
            textField.requestFocusInWindow();
            textField.setText("");

            //Select the new item and make it visible.
            selectedList.setSelectedIndex(index);
            selectedList.ensureIndexIsVisible(index);
        }

        boolean alreadyInList(String name) {
            return listModel.contains(name);
        }

        //Required by DocumentListener.
        public void insertUpdate(DocumentEvent e) {
            enableButton();
        }

        //Required by DocumentListener.
        public void removeUpdate(DocumentEvent e) {
            handleEmptyTextField(e);
        }

        //Required by DocumentListener.
        public void changedUpdate(DocumentEvent e) {
            if (!handleEmptyTextField(e)) {
                enableButton();
            }
        }

        private void enableButton() {
            if (!alreadyEnabled) {
                button.setEnabled(true);
            }
        }

        private boolean handleEmptyTextField(DocumentEvent e) {
            if (e.getDocument().getLength() <= 0) {
                button.setEnabled(false);
                alreadyEnabled = false;
                return true;
            }
            return false;
        }
    }

    class RemoveListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int index = selectedList.getSelectedIndex();
            listModel.remove(index);

            int size = listModel.getSize();

            if (size == 0) {
                removeButton.setEnabled(false);

            } else { //Select an index.
                if (index == listModel.getSize()) {
                    //removed item in last position
                    index--;
                }
                selectedList.setSelectedIndex(index);
                selectedList.ensureIndexIsVisible(index);
            }
        }
    }
}

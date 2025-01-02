import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Timer;

public class TaskManager extends JFrame {
    private JList<Task> taskList;
    private DefaultListModel<Task> listModel;
    private Timer timer;
    private Random random;

    public TaskManager() {
        super("任务管理系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);

        random = new Random();
        timer = new Timer();
        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);

        // 自定义单元格渲染器
        taskList.setCellRenderer(new TaskListRenderer());

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(taskList), BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("添加任务");
        JButton deleteButton = new JButton("删除任务");
        JButton toggleButton = new JButton("标记完成/未完成");
        JButton viewButton = new JButton("查看详情");

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(toggleButton);
        buttonPanel.add(viewButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 添加任务按钮事件
        addButton.addActionListener(e -> showAddTaskDialog());

        // 删除任务按钮事件
        deleteButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                Task task = listModel.getElementAt(selectedIndex);
                task.cancelTimer();
                listModel.remove(selectedIndex);
            }
        });

        // 切换完成状态按钮事件
        toggleButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                Task task = listModel.getElementAt(selectedIndex);
                task.toggleCompleted();
                taskList.repaint();
            }
        });

        // 查看详情按钮事件
        viewButton.addActionListener(e -> {
            int selectedIndex = taskList.getSelectedIndex();
            if (selectedIndex != -1) {
                Task task = listModel.getElementAt(selectedIndex);
                showTaskDetails(task);
            }
        });

        add(mainPanel);
        startRandomReminders();
    }

    private void showTaskDetails(Task task) {
        JDialog dialog = new JDialog(this, "任务详情", true);
        dialog.setLayout(new GridLayout(5, 2, 5, 5));

        dialog.add(new JLabel("标题:"));
        dialog.add(new JLabel(task.getTitle()));

        dialog.add(new JLabel("优先级:"));
        dialog.add(new JLabel(task.getPriorityString()));

        dialog.add(new JLabel("提醒类型:"));
        dialog.add(new JLabel(task.isRandomReminder() ? "随机提醒" : "定时提醒 (" + task.getReminderTime() + ")"));

        dialog.add(new JLabel("状态:"));
        dialog.add(new JLabel(task.isCompleted() ? "已完成" : "未完成"));

        dialog.add(new JLabel("描述:"));
        JTextArea descArea = new JTextArea(task.getDescription());
        descArea.setEditable(false);
        dialog.add(new JScrollPane(descArea));

        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddTaskDialog() {
        JDialog dialog = new JDialog(this, "添加新任务", true);
        dialog.setLayout(new GridLayout(7, 2, 5, 5));

        JTextField titleField = new JTextField();
        JTextArea descArea = new JTextArea(3, 20);
        JTextField timeField = new JTextField("HH:mm");
        JCheckBox randomReminder = new JCheckBox();
        JComboBox<Task.Priority> priorityBox = new JComboBox<>(Task.Priority.values());

        dialog.add(new JLabel("任务标题:"));
        dialog.add(titleField);

        dialog.add(new JLabel("任务描述:"));
        dialog.add(new JScrollPane(descArea));

        dialog.add(new JLabel("优先级:"));
        dialog.add(priorityBox);

        dialog.add(new JLabel("提醒时间 (HH:mm):"));
        dialog.add(timeField);

        dialog.add(new JLabel("随机提醒:"));
        dialog.add(randomReminder);

        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> {
            String title = titleField.getText();
            String description = descArea.getText();
            String time = timeField.getText();
            Task.Priority priority = (Task.Priority) priorityBox.getSelectedItem();
            Task task = new Task(title, description, time, randomReminder.isSelected(), priority);
            listModel.addElement(task);
            dialog.dispose();
        });

        dialog.add(new JLabel());
        dialog.add(saveButton);

        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void startRandomReminders() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < listModel.size(); i++) {
                    Task task = listModel.getElementAt(i);
                    if (!task.isCompleted() && task.isRandomReminder() && random.nextInt(100) < 10) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(TaskManager.this,
                                        "随机提醒: " + task.getTitle() + "\n优先级: " + task.getPriorityString(),
                                        "提醒",
                                        JOptionPane.INFORMATION_MESSAGE)
                        );
                    }
                }
            }
        }, 0, 5 * 60 * 1000);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TaskManager().setVisible(true);
        });
    }
}

class Task {
    public enum Priority {
        HIGH("高"), MEDIUM("中"), LOW("低");

        private String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private String title;
    private String description;
    private String reminderTime;
    private boolean randomReminder;
    private Priority priority;
    private boolean completed;
    private Timer timer;

    public Task(String title, String description, String reminderTime, boolean randomReminder, Priority priority) {
        this.title = title;
        this.description = description;
        this.reminderTime = reminderTime;
        this.randomReminder = randomReminder;
        this.priority = priority;
        this.completed = false;

        if (!randomReminder) {
            scheduleFixedTimeReminder();
        }
    }

    private void scheduleFixedTimeReminder() {
        timer = new Timer();
        Calendar calendar = Calendar.getInstance();
        String[] timeParts = reminderTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!completed) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(null,
                                    "定时提醒: " + title + "\n优先级: " + getPriorityString(),
                                    "提醒",
                                    JOptionPane.INFORMATION_MESSAGE)
                    );
                }
            }
        }, calendar.getTime(), 24 * 60 * 60 * 1000);
    }

    public void toggleCompleted() {
        completed = !completed;
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getReminderTime() { return reminderTime; }
    public boolean isRandomReminder() { return randomReminder; }
    public Priority getPriority() { return priority; }
    public String getPriorityString() { return priority.toString(); }
    public boolean isCompleted() { return completed; }

    @Override
    public String toString() {
        return String.format("[%s] %s %s",
                priority.toString(),
                title,
                completed ? "✓" : ""
        );
    }
}

class TaskListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Task) {
            Task task = (Task) value;

            // 设置任务优先级的颜色
            if (!isSelected) {
                switch (task.getPriority()) {
                    case HIGH:
                        setForeground(Color.RED);
                        break;
                    case MEDIUM:
                        setForeground(Color.BLUE);
                        break;
                    case LOW:
                        setForeground(Color.GREEN.darker());
                        break;
                }
            }

            // 如果任务已完成，添加删除线效果
            if (task.isCompleted()) {
                setText("<html><strike>" + getText() + "</strike></html>");
            }
        }

        return c;
    }
}
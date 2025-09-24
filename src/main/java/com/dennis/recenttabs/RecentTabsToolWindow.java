package com.dennis.recenttabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

public class RecentTabsToolWindow implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        RecentTabsPanel recentTabsPanel = new RecentTabsPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(recentTabsPanel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Register listener for file editor events
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new RecentTabsFileEditorListener(recentTabsPanel)
        );
    }

    private static class RecentTabsPanel extends JPanel {
        private final DefaultListModel<VirtualFile> listModel;
        private final JList<VirtualFile> fileList;
        private final List<VirtualFile> recentFiles;
        private static final int MAX_RECENT_FILES = 50;
        private int hoveredIndex = -1;

        public RecentTabsPanel(Project project) {
            this.recentFiles = new LinkedList<>();
            this.listModel = new DefaultListModel<>();

            setLayout(new BorderLayout());


            // Configure the file list
            fileList = new JBList<>(listModel) {
                @Override
                public int locationToIndex(Point location) {
                    int idx = super.locationToIndex(location);
                    if (idx == -1) return -1;
                    Rectangle bounds = getCellBounds(idx, idx);
                    return (bounds != null && bounds.contains(location)) ? idx : -1;
                }
            };
            fileList.setCellRenderer(new RecentTabsListCellRenderer());

            // Add mouse listener for single-click functionality
            fileList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int index = fileList.locationToIndex(e.getPoint());
                    if (index < 0 || index >= listModel.getSize()) {
                        return;
                    }
                    Rectangle cellBounds = fileList.getCellBounds(index, index);
                    if (cellBounds == null || !cellBounds.contains(e.getPoint())) {
                        // Click happened in an empty area; do nothing
                        return;
                    }
                    VirtualFile file = listModel.getElementAt(index);
                    if (file != null && file.isValid()) {
                        // Regular click - open the file normally
                        FileEditorManager.getInstance(project).openFile(file, true);
                    } else if (file != null) {
                        // File no longer exists, remove it from the list
                        recentFiles.remove(file);
                        updateListModel();
                    }
                }
            });

            // Single-click selection listener - just for highlighting, no action needed
            fileList.addListSelectionListener(e -> {
                // Empty implementation to prevent any unexpected behavior
            });

            // Add mouse motion listener for hover effect
            fileList.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int index = fileList.locationToIndex(e.getPoint());
                    int newHovered = -1;
                    if (index >= 0) {
                        Rectangle cellBounds = fileList.getCellBounds(index, index);
                        if (cellBounds != null && cellBounds.contains(e.getPoint())) {
                            newHovered = index;
                        }
                    }
                    if (newHovered != hoveredIndex) {
                        hoveredIndex = newHovered;
                        fileList.putClientProperty("recentTabs.hoveredIndex", hoveredIndex);
                        fileList.repaint();
                    }
                }
            });

            // Add mouse listener to reset hover state when mouse exits the list
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    if (hoveredIndex != -1) {
                        hoveredIndex = -1;
                        fileList.putClientProperty("recentTabs.hoveredIndex", hoveredIndex);
                        fileList.repaint();
                    }
                }
            });

            JScrollPane scrollPane = new JBScrollPane(fileList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(scrollPane, BorderLayout.CENTER);

            // Initialize with currently open files
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
            for (VirtualFile file : openFiles) {
                addRecentFile(file);
            }
        }

        public void addRecentFile(VirtualFile file) {
            if (file == null) return;

            // Remove if already in the list
            recentFiles.remove(file);

            // Add to the beginning of a list
            recentFiles.add(0, file);

            // Trim list if needed
            while (recentFiles.size() > MAX_RECENT_FILES) {
                recentFiles.remove(recentFiles.size() - 1);
            }

            // Update UI
            updateListModel();
        }

        private void updateListModel() {
            listModel.clear();

            // Create a temporary list to store files that need to be removed
            List<VirtualFile> filesToRemove = new LinkedList<>();

            for (VirtualFile file : recentFiles) {
                if (file.isValid()) {
                    listModel.addElement(file);
                } else {
                    // File no longer exists, mark for removal
                    filesToRemove.add(file);
                }
            }

            // Remove invalid files from the recent files list
            if (!filesToRemove.isEmpty()) {
                recentFiles.removeAll(filesToRemove);
            }
        }
    }

    private static class RecentTabsListCellRenderer extends ColoredListCellRenderer<VirtualFile> {
        private static final int BORDER_SIZE = 5;

        @Override
        protected void customizeCellRenderer(
                @NotNull JList<? extends VirtualFile> list,
                VirtualFile value,
                int index,
                boolean selected,
                boolean hasFocus) {
            setBorder(JBUI.Borders.empty(BORDER_SIZE));
            setOpaque(true);

            int hoveredIndex = -1;
            Object prop = list.getClientProperty("recentTabs.hoveredIndex");
            if (prop instanceof Integer) {
                hoveredIndex = (Integer) prop;
            }

            Color bg = UIUtil.getListBackground(selected, hasFocus);
            Color fg = UIUtil.getListForeground(selected, hasFocus);

            if (!selected && index == hoveredIndex) {
                bg = UIUtil.getPanelBackground().darker();
            }

            setBackground(bg);
            setForeground(fg);

            if (value != null) {
                setIcon(value.getFileType().getIcon());
                append(value.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                setToolTipText(value.getPath());
            }
        }
    }

    private record RecentTabsFileEditorListener(RecentTabsPanel recentTabsPanel) implements FileEditorManagerListener {

        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            recentTabsPanel.addRecentFile(file);
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            // We still keep closed files in the recent list
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (event.getNewFile() != null) {
                recentTabsPanel.addRecentFile(event.getNewFile());
            }
        }
    }
}

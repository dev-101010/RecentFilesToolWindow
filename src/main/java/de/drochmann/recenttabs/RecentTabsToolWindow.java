package de.drochmann.recenttabs;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecentTabsToolWindow implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        RecentTabsPanel panel = new RecentTabsPanel(project);

        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        // Tied to the tool window's lifetime so the subscription is released when it is disposed.
        project.getMessageBus()
                .connect(toolWindow.getDisposable())
                .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new RecentTabsListener(panel));
    }

    private static final class RecentTabsPanel extends JPanel {
        private static final int MAX_RECENT_FILES = 50;

        private final Project project;
        private final DefaultListModel<VirtualFile> listModel = new DefaultListModel<>();
        private final List<VirtualFile> recentFiles = new ArrayList<>();
        private final JBList<VirtualFile> fileList;

        private int hoveredIndex = -1;

        RecentTabsPanel(@NotNull Project project) {
            super(new BorderLayout());
            this.project = project;

            fileList = new JBList<>(listModel);
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            fileList.setCellRenderer(new RecentTabsListCellRenderer(this::hoveredIndex));

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    openFile(fileAt(e.getPoint()));
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    setHoveredIndex(indexAt(e.getPoint()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHoveredIndex(-1);
                }
            };
            fileList.addMouseListener(mouseAdapter);
            fileList.addMouseMotionListener(mouseAdapter);

            // Keyboard equivalent of a click, so the list is usable without a mouse.
            fileList.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "recentTabs.openSelected");
            fileList.getActionMap().put("recentTabs.openSelected", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = fileList.getSelectedIndex();
                    openFile(index >= 0 && index < listModel.getSize() ? listModel.getElementAt(index) : null);
                }
            });

            JScrollPane scrollPane = new JBScrollPane(fileList);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            add(scrollPane, BorderLayout.CENTER);

            for (VirtualFile file : FileEditorManager.getInstance(project).getOpenFiles()) {
                addRecentFile(file);
            }
        }

        private int hoveredIndex() {
            return hoveredIndex;
        }

        private void setHoveredIndex(int index) {
            if (index != hoveredIndex) {
                hoveredIndex = index;
                fileList.repaint();
            }
        }

        /** Returns the row index at {@code point}, or -1 when the point falls into empty space. */
        private int indexAt(@NotNull Point point) {
            int index = fileList.locationToIndex(point);
            if (index < 0 || index >= listModel.getSize()) {
                return -1;
            }
            Rectangle bounds = fileList.getCellBounds(index, index);
            return bounds != null && bounds.contains(point) ? index : -1;
        }

        private @Nullable VirtualFile fileAt(@NotNull Point point) {
            int index = indexAt(point);
            return index < 0 ? null : listModel.getElementAt(index);
        }

        private void openFile(@Nullable VirtualFile file) {
            if (file == null) {
                return;
            }
            if (file.isValid()) {
                FileEditorManager.getInstance(project).openFile(file, true);
            } else {
                recentFiles.remove(file);
                updateListModel();
            }
        }

        void addRecentFile(@Nullable VirtualFile file) {
            if (file == null) {
                return;
            }
            recentFiles.remove(file);
            recentFiles.add(0, file);
            while (recentFiles.size() > MAX_RECENT_FILES) {
                recentFiles.remove(recentFiles.size() - 1);
            }
            updateListModel();
        }

        private void updateListModel() {
            recentFiles.removeIf(file -> !file.isValid());

            VirtualFile selected = fileList.getSelectedValue();
            listModel.clear();
            listModel.addAll(recentFiles);

            int selectedIndex = selected == null ? -1 : recentFiles.indexOf(selected);
            if (selectedIndex >= 0) {
                fileList.setSelectedIndex(selectedIndex);
            }
        }
    }

    private static final class RecentTabsListCellRenderer extends ColoredListCellRenderer<VirtualFile> {
        private static final int BORDER_SIZE = 5;

        private final java.util.function.IntSupplier hoveredIndex;

        RecentTabsListCellRenderer(@NotNull java.util.function.IntSupplier hoveredIndex) {
            this.hoveredIndex = hoveredIndex;
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends VirtualFile> list,
                                             VirtualFile value,
                                             int index,
                                             boolean selected,
                                             boolean hasFocus) {
            setBorder(JBUI.Borders.empty(BORDER_SIZE));
            setOpaque(true);

            Color background = selected || index != hoveredIndex.getAsInt()
                    ? JBUI.CurrentTheme.List.background(selected, hasFocus)
                    : JBUI.CurrentTheme.List.Hover.background(hasFocus);
            setBackground(background);
            setForeground(JBUI.CurrentTheme.List.foreground(selected, hasFocus));

            if (value != null) {
                setIcon(value.getFileType().getIcon());
                append(value.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                setToolTipText(value.getPath());
            }
        }
    }

    private record RecentTabsListener(RecentTabsPanel panel) implements FileEditorManagerListener {

        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            panel.addRecentFile(file);
        }

        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            panel.addRecentFile(event.getNewFile());
        }
    }
}

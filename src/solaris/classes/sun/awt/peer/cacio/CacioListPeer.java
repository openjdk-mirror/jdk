package sun.awt.peer.cacio;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.peer.ListPeer;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

final class CacioListPeer extends CacioComponentPeer<List, JScrollPane> implements ListPeer {

    private JList list;

    public CacioListPeer(List awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
    }

    @Override
    JScrollPane initSwingComponent() {
        list = new JList(new DefaultListModel());
        JScrollPane pane = new JScrollPane(list);
        return pane;
    }

    @Override
    void postInitSwingComponent() {
        super.postInitSwingComponent();
        // Add initial items.
        List theList = getAWTComponent();
        int itemCount = theList.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            add(theList.getItem(i), i);
        }
        setMultipleMode(theList.isMultipleMode());
        list.addListSelectionListener(new SelectionListener());
    }

    private DefaultListModel getModel() {
        DefaultListModel m = (DefaultListModel) list.getModel();
        return m;
    }

    @Override
    public void add(String item, int index) {
        if (index < 0) {
            getModel().addElement(item);
        } else {
            getModel().add(index, item);
        }
    }

    @Override
    public void delItems(int start, int end) {
        getModel().removeRange(start, end);
    }

    @Override
    public Dimension getMinimumSize(int rows) {
        FontMetrics fm = getFontMetrics(getAWTComponent().getFont());
        return new Dimension(20 + fm.stringWidth("0123456789abcde"),
                             (fm.getHeight() * rows));
    }

    @Override
    public Dimension getMinimumSize() {
        return getMinimumSize(5);
    }

    @Override
    public Dimension getPreferredSize(int rows) {
        Dimension minSize = getMinimumSize(5);
        Dimension actualSize = getSwingComponent().getPreferredSize();
        if (actualSize.width < minSize.width) actualSize.width = minSize.width;
        if (actualSize.height < actualSize.height) actualSize.height = minSize.height;
        return actualSize;
    }
    
    @Override
    public Dimension getPreferredSize() {
        int rows = getModel().getSize();
        return getPreferredSize((rows < 5) ? 5 : rows);
    }

    @Override
    public void makeVisible(int index) {
        list.ensureIndexIsVisible(index);
    }

    @Override
    public void select(int index) {
        list.setSelectedIndex(index);
    }

    @Override
    public void deselect(int index) {
        list.removeSelectionInterval(index, index);
    }

    @Override
    public void removeAll() {        
        ListSelectionModel m = list.getSelectionModel();
        m.clearSelection();
    }

    @Override
    public int[] getSelectedIndexes() {
        return list.getSelectedIndices();
    }

    @Override
    public void setMultipleMode(boolean multiple) {
        int mode;
        if (multiple) {
            mode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
        } else {
            mode = ListSelectionModel.SINGLE_SELECTION;
        }
        list.setSelectionMode(mode);
    }

    @Override
    protected void handleMouseEvent(MouseEvent e)
    {
        super.handleMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_RELEASED && e.getClickCount() == 2) {
              getToolkit().getSystemEventQueue().postEvent(new ActionEvent(getAWTComponent(),ActionEvent.ACTION_PERFORMED, ""+getModel().getElementAt(list.locationToIndex(e.getPoint()))));
        }
    }

    public void setEnabled(boolean e) {
        super.setEnabled(e);
        list.setEnabled(e);
    }

    class SelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            for (int index = e.getFirstIndex(); index < e.getLastIndex(); index ++ )
            {
              getToolkit().getSystemEventQueue().postEvent(new ItemEvent(getAWTComponent(),ItemEvent.ITEM_STATE_CHANGED , getModel().getElementAt(index) ,list.isSelectedIndex(index)? ItemEvent.SELECTED:ItemEvent.DESELECTED));
            }
        }

    }

}

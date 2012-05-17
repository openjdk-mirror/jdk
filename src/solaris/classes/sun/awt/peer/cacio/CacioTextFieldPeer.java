package sun.awt.peer.cacio;

import java.awt.Dimension;
import java.awt.TextField;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.im.InputMethodRequests;

import java.awt.peer.TextFieldPeer;

import javax.swing.JPasswordField;

class CacioTextFieldPeer extends CacioComponentPeer<TextField, JPasswordField>
    implements TextFieldPeer {

    CacioTextFieldPeer(TextField awtC, PlatformWindowFactory pwf) {
        super(awtC, pwf);
    }

    @Override
    JPasswordField initSwingComponent() {
        
        TextField textField = getAWTComponent();
        JPasswordField swingComponent = new JPasswordField();
        swingComponent.setText(textField.getText());
        swingComponent.setColumns(textField.getColumns());
        swingComponent.setEchoChar(textField.getEchoChar());
        swingComponent.setEditable(textField.isEditable());
        swingComponent.select(textField.getSelectionStart(),
                              textField.getSelectionEnd());
        
        swingComponent.addActionListener(new SwingTextFieldListener());
        return swingComponent;
    }
    
    class SwingTextFieldListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {

            TextField textField = getAWTComponent();
            
            ActionListener[] listeners = textField.getActionListeners();
            if (listeners.length == 0)
                return;
            
            ActionEvent ev =
                new ActionEvent(textField, 
                                ActionEvent.ACTION_PERFORMED,
                                event.getActionCommand());
            
            for (ActionListener listener : listeners) {
                listener.actionPerformed(ev);
            }
        }
    
    }
    
    /* ***** Peer specific implementation ***** */
    
    @Override
    public Dimension getMinimumSize(int columns) {

        return getSwingComponent().getMinimumSize();

    }

    @Override
    public Dimension getPreferredSize(int columns) {
        
        return getSwingComponent().getPreferredSize();
    }

    @Override
    public void setEchoChar(char echoChar) {
        
        getSwingComponent().setEchoChar(echoChar);
    }

    @Override
    public int getCaretPosition() {
        
        return getSwingComponent().getCaretPosition();
    }

    @Override
    public InputMethodRequests getInputMethodRequests() {
        
        return getSwingComponent().getInputMethodRequests();
    }

    @Override
    public int getSelectionEnd() {
        
        return getSwingComponent().getSelectionEnd();
    }

    @Override
    public int getSelectionStart() {
        
        return getSwingComponent().getSelectionStart();
    }

    @Override
    public String getText() {
        
        return getSwingComponent().getText();
    }

    @Override
    public void select(int selStart, int selEnd) {
       
        getSwingComponent().select(selStart, selEnd);
    }

    @Override
    public void setCaretPosition(int pos) {
        
        getSwingComponent().setCaretPosition(pos);
    }

    @Override
    public void setEditable(boolean editable) {
        
        getSwingComponent().setEditable(editable);
    }

    @Override
    public void setText(String l) {
        
        getSwingComponent().setText(l);
    }
}

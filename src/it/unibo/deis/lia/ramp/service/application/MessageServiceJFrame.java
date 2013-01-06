/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileSharingServerJFrame.java
 *
 * Created on 7-nov-2009, 11.00.19
 */

package it.unibo.deis.lia.ramp.service.application;

import java.util.*;

/**
 *
 * @author useruser
 */
public class MessageServiceJFrame extends javax.swing.JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MessageService ms;
    
    /** Creates new form FileSharingServerJFrame */
    public MessageServiceJFrame(MessageService ms) {
        this.ms=ms;
        initComponents();
        new RefreshMessages(this).start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonGetReceivedMessages = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaMessageList = new javax.swing.JTextArea();
        jButtonResetReceivedMessages = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MessageService");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jButtonGetReceivedMessages.setText("get received messages");
        jButtonGetReceivedMessages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGetReceivedMessagesActionPerformed(evt);
            }
        });

        jTextAreaMessageList.setColumns(20);
        jTextAreaMessageList.setEditable(false);
        jTextAreaMessageList.setRows(5);
        jScrollPane1.setViewportView(jTextAreaMessageList);

        jButtonResetReceivedMessages.setText("reset received messages");
        jButtonResetReceivedMessages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetReceivedMessagesActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonGetReceivedMessages)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 95, Short.MAX_VALUE)
                        .addComponent(jButtonResetReceivedMessages)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonGetReceivedMessages)
                    .addComponent(jButtonResetReceivedMessages))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonGetReceivedMessagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGetReceivedMessagesActionPerformed
        String res="";
        Vector<String> messageList=ms.getReceivedMessages();
        for(String mess : messageList){
            res+=mess+"\n";
        }
        this.jTextAreaMessageList.setText(res);
    }//GEN-LAST:event_jButtonGetReceivedMessagesActionPerformed

    private void jButtonResetReceivedMessagesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetReceivedMessagesActionPerformed
        ms.resetReceivedMessages();
        this.jButtonGetReceivedMessagesActionPerformed(null);
    }//GEN-LAST:event_jButtonResetReceivedMessagesActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // do nothing
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        System.out.println("FileSharingServiceJFrame: formWindowClosing");
        ms.stopService();
    }//GEN-LAST:event_formWindowClosing
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGetReceivedMessages;
    private javax.swing.JButton jButtonResetReceivedMessages;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaMessageList;
    // End of variables declaration//GEN-END:variables

    private class RefreshMessages extends Thread{
        private MessageServiceJFrame msjf;
        private RefreshMessages(MessageServiceJFrame msjf){
            this.msjf=msjf;
        }
        @Override
        public void run(){
            while(ms.isActive()){
                msjf.jButtonGetReceivedMessagesActionPerformed(null);
                try{
                    sleep(1000);
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
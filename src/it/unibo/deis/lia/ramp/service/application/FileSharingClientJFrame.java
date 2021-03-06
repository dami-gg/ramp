/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * FileSharingClientJFrame.java
 *
 * Created on 7-nov-2009, 11.30.30
 */

package it.unibo.deis.lia.ramp.service.application;

import it.unibo.deis.lia.ramp.service.management.*;

import java.util.*;
import javax.swing.*;

/**
 *
 * @author useruser
 */
public class FileSharingClientJFrame extends javax.swing.JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Vector<ServiceResponse> availableServices;

    private FileSharingClient fsc;
    public FileSharingClientJFrame(FileSharingClient fsc) {
        initComponents();

        this.fsc=fsc;
        String[] empty=new String[0];
        DefaultComboBoxModel dcm = new DefaultComboBoxModel(empty);
        jComboBoxRemoteServices.setModel(dcm);
        jComboBoxRemoteFileList.setModel(dcm);
        jComboBoxLocalFileList.setModel(dcm);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonFindRemoteServices = new javax.swing.JButton();
        jScrollPaneRemoteServices = new javax.swing.JScrollPane();
        jTextAreaRemoteServices = new javax.swing.JTextArea();
        jComboBoxRemoteServices = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldTTL = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldTimeout = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldServiceAmount = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jButtonRequireFileList = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaRemoteFileList = new javax.swing.JTextArea();
        jComboBoxRemoteFileList = new javax.swing.JComboBox();
        jButtonRequireSelectedFile = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jButtonRefreshLocalFileList = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaLocalFileList = new javax.swing.JTextArea();
        jComboBoxLocalFileList = new javax.swing.JComboBox();
        jButtonSendSelectedFile = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("FileSharingClient");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jButtonFindRemoteServices.setText("find services");
        jButtonFindRemoteServices.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFindRemoteServicesActionPerformed(evt);
            }
        });

        jTextAreaRemoteServices.setColumns(20);
        jTextAreaRemoteServices.setEditable(false);
        jTextAreaRemoteServices.setRows(5);
        jScrollPaneRemoteServices.setViewportView(jTextAreaRemoteServices);

        jComboBoxRemoteServices.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxRemoteServices.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxRemoteServicesActionPerformed(evt);
            }
        });

        jLabel1.setText("TTL");

        jTextFieldTTL.setText("3");

        jLabel2.setText("timeout");

        jTextFieldTimeout.setText("5000");

        jLabel3.setText("serviceAmount");

        jTextFieldServiceAmount.setText("1");
        jTextFieldServiceAmount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldServiceAmountActionPerformed(evt);
            }
        });

        jButtonRequireFileList.setText("require file list");
        jButtonRequireFileList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRequireFileListActionPerformed(evt);
            }
        });

        jTextAreaRemoteFileList.setColumns(20);
        jTextAreaRemoteFileList.setEditable(false);
        jTextAreaRemoteFileList.setRows(5);
        jScrollPane1.setViewportView(jTextAreaRemoteFileList);

        jComboBoxRemoteFileList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButtonRequireSelectedFile.setText("require selected file");
        jButtonRequireSelectedFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRequireSelectedFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButtonRequireFileList, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
            .addComponent(jComboBoxRemoteFileList, 0, 302, Short.MAX_VALUE)
            .addComponent(jButtonRequireSelectedFile, javax.swing.GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jButtonRequireFileList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxRemoteFileList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonRequireSelectedFile)
                .addContainerGap())
        );

        jButtonRefreshLocalFileList.setText("refresh local file list");
        jButtonRefreshLocalFileList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRefreshLocalFileListActionPerformed(evt);
            }
        });

        jTextAreaLocalFileList.setColumns(20);
        jTextAreaLocalFileList.setEditable(false);
        jTextAreaLocalFileList.setRows(5);
        jScrollPane2.setViewportView(jTextAreaLocalFileList);

        jComboBoxLocalFileList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButtonSendSelectedFile.setText("send selected file");
        jButtonSendSelectedFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSendSelectedFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButtonRefreshLocalFileList, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
            .addComponent(jComboBoxLocalFileList, 0, 298, Short.MAX_VALUE)
            .addComponent(jButtonSendSelectedFile, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jButtonRefreshLocalFileList)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxLocalFileList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSendSelectedFile)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPaneRemoteServices, javax.swing.GroupLayout.DEFAULT_SIZE, 606, Short.MAX_VALUE)
                    .addComponent(jComboBoxRemoteServices, 0, 606, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButtonFindRemoteServices, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 186, Short.MAX_VALUE)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldTTL, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldServiceAmount, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonFindRemoteServices)
                    .addComponent(jTextFieldServiceAmount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldTimeout, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldTTL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPaneRemoteServices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxRemoteServices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonFindRemoteServicesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFindRemoteServicesActionPerformed
        jTextAreaRemoteFileList.setText("");
        jTextAreaRemoteServices.setText("");

        String [] emptyList = {};
        DefaultComboBoxModel emptyDcbm = new DefaultComboBoxModel(emptyList);
        jComboBoxRemoteFileList.setModel(emptyDcbm);
        jComboBoxRemoteServices.setModel(emptyDcbm);
        
        try{
            int ttl=Integer.parseInt(jTextFieldTTL.getText());
            int timeout=Integer.parseInt(jTextFieldTimeout.getText());
            int serviceAmount=Integer.parseInt(jTextFieldServiceAmount.getText());
            availableServices = fsc.findFileSharingService(ttl, timeout, serviceAmount);
            String text="";
            String[] items=new String[availableServices.size()];
            for (int i=0; i<availableServices.size(); i++){
                text+=availableServices.elementAt(i)+"\n";
                items[i]=""+availableServices.elementAt(i);
            }
            jTextAreaRemoteServices.setText(text);
            DefaultComboBoxModel dcm = new DefaultComboBoxModel(items);
            jComboBoxRemoteServices.setModel(dcm);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButtonFindRemoteServicesActionPerformed

    private void jButtonRequireFileListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRequireFileListActionPerformed
        try{
            int index = jComboBoxRemoteServices.getSelectedIndex();
            if(index!=-1){
                ServiceResponse service = availableServices.elementAt(index);
                String[] files = fsc.getRemoteFileList(service);
                String text="";
                String[] items=new String[files.length];
                for (int i=0; i<files.length; i++){
                    text+=files[i]+"\n";
                    items[i]=files[i];
                }
                jTextAreaRemoteFileList.setText(text);
                DefaultComboBoxModel dcm = new DefaultComboBoxModel(items);
                jComboBoxRemoteFileList.setModel(dcm);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButtonRequireFileListActionPerformed

    private void jComboBoxRemoteServicesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxRemoteServicesActionPerformed
        this.jTextAreaRemoteFileList.setText("");
        String[] empty={};
        DefaultComboBoxModel dcm = new DefaultComboBoxModel(empty);
        jComboBoxRemoteFileList.setModel(dcm);
    }//GEN-LAST:event_jComboBoxRemoteServicesActionPerformed

    private void jButtonRequireSelectedFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRequireSelectedFileActionPerformed
        try{
            int index = jComboBoxRemoteServices.getSelectedIndex();
            if(index!=-1){
                final ServiceResponse service = availableServices.elementAt(index);
                final String file = jComboBoxRemoteFileList.getSelectedItem().toString();
                //fsc.getRemoteFile(service, file);
                Thread t = new Thread(
                        new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    fsc.getRemoteFile(service, file);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                );
                t.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButtonRequireSelectedFileActionPerformed

    private void jTextFieldServiceAmountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldServiceAmountActionPerformed
        // do nothing...
    }//GEN-LAST:event_jTextFieldServiceAmountActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        fsc.stopClient();
    }//GEN-LAST:event_formWindowClosing

    private void jButtonRefreshLocalFileListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRefreshLocalFileListActionPerformed
        System.out.println("FileSharingClientJFrame: jButtonRefreshLocalFileListActionPerformed");
        String[] files = fsc.getLocalFileList();
        String text="";
        String[] items=new String[files.length];
        for(int i=0; i<files.length; i++){
            text+=files[i]+"\n";
            items[i]=files[i];
        }
        jTextAreaLocalFileList.setText(text);
        DefaultComboBoxModel dcm = new DefaultComboBoxModel(items);
        jComboBoxLocalFileList.setModel(dcm);
    }//GEN-LAST:event_jButtonRefreshLocalFileListActionPerformed

    private void jButtonSendSelectedFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSendSelectedFileActionPerformed
        try{
            int index = jComboBoxRemoteServices.getSelectedIndex();
            if(index!=-1 && jComboBoxLocalFileList.getSelectedItem()!=null){
                final ServiceResponse service = availableServices.elementAt(index);
                final String file = jComboBoxLocalFileList.getSelectedItem().toString();
                Thread t = new Thread(
                        new Runnable(){
                            @Override
                            public void run() {
                                try {
                                    fsc.sendLocalFile(service, file);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                );
                t.start();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButtonSendSelectedFileActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonFindRemoteServices;
    private javax.swing.JButton jButtonRefreshLocalFileList;
    private javax.swing.JButton jButtonRequireFileList;
    private javax.swing.JButton jButtonRequireSelectedFile;
    private javax.swing.JButton jButtonSendSelectedFile;
    private javax.swing.JComboBox jComboBoxLocalFileList;
    private javax.swing.JComboBox jComboBoxRemoteFileList;
    private javax.swing.JComboBox jComboBoxRemoteServices;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneRemoteServices;
    private javax.swing.JTextArea jTextAreaLocalFileList;
    private javax.swing.JTextArea jTextAreaRemoteFileList;
    private javax.swing.JTextArea jTextAreaRemoteServices;
    private javax.swing.JTextField jTextFieldServiceAmount;
    private javax.swing.JTextField jTextFieldTTL;
    private javax.swing.JTextField jTextFieldTimeout;
    // End of variables declaration//GEN-END:variables

}

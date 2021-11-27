package net.homework.blockchain.client.swing;
import net.homework.blockchain.client.UserImpl;
import net.homework.blockchain.entity.Transaction;
import net.homework.blockchain.util.CryptoUtils;

import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class TabbedPane extends JPanel{
    UserImpl userImpl = new UserImpl();
    public static void main(String[] args){
        JFrame frame = new JFrame("Client");
        frame.setBounds(10,10,800,800);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new TabbedPane(),BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
    public TabbedPane(){
        super(new GridLayout(1,1));
        JTabbedPane jTabbedPane = new JTabbedPane();
        jTabbedPane.setSize(800,700);
        JComponent genPriKeyPaneCom = genPriKeyPane();
        jTabbedPane.addTab("生成私钥",genPriKeyPaneCom);
        jTabbedPane.setMnemonicAt(0,KeyEvent.VK_1);
        genPriKeyPaneCom.setSize(800,600);
        JComponent getOutPutPaneCom = getOutPutPane();
        jTabbedPane.addTab("进行交易",getOutPutPaneCom);
        jTabbedPane.setMnemonicAt(1,KeyEvent.VK_2);
        getOutPutPaneCom.setSize(800,600);
        add(jTabbedPane);
    }
    public JComponent genPriKeyPane(){
        JPanel panel = new JPanel();
        panel.setSize(800,500);
        JButton genPriKeyButton = new JButton("生成私钥");    //创建JButton对象
        genPriKeyButton.setSize(800,400);
        genPriKeyButton.setFont(new Font("黑体",Font.BOLD,50));    //修改字体样式
        genPriKeyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userImpl.savePrivateKey(userImpl.generatePrivateKey());
                JOptionPane.showMessageDialog(panel,"生成成功！","提示",JOptionPane.PLAIN_MESSAGE);
            }
        });
        panel.setLayout(new GridLayout(1,1));
        panel.add(genPriKeyButton);
        return panel;
    }
    public JComponent getOutPutPane(){
        JPanel panel = new JPanel();
        panel.setSize(800,500);
        String[] name = {"数量","公钥"};
        String[][] strings = new String[25][2];
        for (int i=0;i<25;i++){
            strings[i][0]="0";
        }
        JTable jTable = new JTable(strings,name);
        panel.add(new JScrollPane(jTable));
        JButton submit = new JButton("确认");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Map<byte[], Long> recipientsWithAmount = new HashMap<>();
                for(int i=0;i<25;i++){
                    long value = Long.parseLong((String) jTable.getValueAt(i,0));
                    if (value!=0){
                        recipientsWithAmount.put(CryptoUtils.getPublicKeyHashFromAddress((String) jTable.getValueAt(i,1)),value);
                    }
                }
                Transaction transaction = userImpl.assembleTx(recipientsWithAmount);
                userImpl.broadcastTx(transaction);
                JOptionPane.showMessageDialog(panel,"用户名或密码错误！","错误 ",0);
                for(int i=0;i<25;i++){
                    jTable.setValueAt("0",i,0);
                    jTable.setValueAt("",i,1);
                }
            }
        });
        panel.add(submit);
        return panel;
    }
}

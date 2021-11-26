package net.homework.blockchain.client.swing;

import lombok.SneakyThrows;
import net.homework.blockchain.client.UserImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Frame extends JFrame {
    public Frame()
        {
            UserImpl userImpl = new UserImpl();
            setTitle("Cilent");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setBounds(100,100,400,200);
            JPanel mainPanel=new JPanel();
            mainPanel.setBorder(new EmptyBorder(5,5,5,5));
            mainPanel.setLayout(new BorderLayout(0,0));
            setContentPane(mainPanel);
            JButton genPriKey=new JButton("生成私钥");    //创建JButton对象
            genPriKey.setFont(new Font("黑体",Font.BOLD,16));    //修改字体样式
            genPriKey.addActionListener(new ActionListener() {
                @SneakyThrows
                public void actionPerformed(ActionEvent e) {
                    userImpl.savePrivateKey(userImpl.generatePrivateKey());
                }
            });
            mainPanel.add(genPriKey);

        }
        public static void main(String[] args) {
            Frame frame=new Frame();
            frame.setVisible(true);
        }
    }
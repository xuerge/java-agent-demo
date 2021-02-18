package com.eben.agentboot;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;

public class AgentBoot {
    public static void main(String[] args) {
        String agentFilePath = args[0];
        String pid = args[1];

        //iterate all jvms and get the first one that matches our application name
        File agentFile = new File(agentFilePath);
        try {
            VirtualMachine jvm = VirtualMachine.attach(pid);
            jvm.loadAgent(agentFile.getAbsolutePath());
            jvm.detach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

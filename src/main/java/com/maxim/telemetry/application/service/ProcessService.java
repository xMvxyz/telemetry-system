package com.maxim.telemetry.application.service;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import org.springframework.stereotype.Service;

@Service
public class ProcessService {

    public String getActiveWindowTitle() {
        char[] buffer = new char[1024 * 2];
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
        String title = Native.toString(buffer);
        
        if (title.isEmpty()) return "Escritorio / Sistema";
        
        // Limpieza basica para nombres largos
        if (title.length() > 30) {
            title = title.substring(0, 27) + "...";
        }
        return title;
    }

    public int getForegroundProcessId() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);
        return pid.getValue();
    }
}

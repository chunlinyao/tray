/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package qz.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

import static java.lang.Thread.sleep;

public class ImeUtilities {
    public static int IMC_GETOPENSTATUS = 0x0005;
    public static int IMC_SETOPENSTATUS = 0x0006;
    public static int WM_IME_CONTROL = 0x0283;
    public static byte VK_CAPITAL = 0x14;
    public static long KEYEVENTF_EXTENDEDKEY = 0x0001;
    public static long KEYEVENTF_KEYUP = 0x0002;
    public static long IME_CHOTKEY_IME_NONIME_TOGGLE = 0x10;
    public static int MAPVK_VK_TO_VSC = 0x0;

    private static LibUser32 user32 = Native.load("user32", LibUser32.class);
    private static LibIMM imm = Native.load("imm32", LibIMM.class);

    static public void setCapsLock(boolean bState) {
        boolean current = getCapsLock();
        if (bState != current ) {
            user32.keybd_event(VK_CAPITAL, (byte) user32.MapVirtualKeyA(VK_CAPITAL, MAPVK_VK_TO_VSC), KEYEVENTF_EXTENDEDKEY | 0, 0L);
            user32.keybd_event(VK_CAPITAL, (byte) user32.MapVirtualKeyA(VK_CAPITAL, MAPVK_VK_TO_VSC), KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, 0L);
        }
    }

    static public boolean getCapsLock() {
        int current = user32.GetKeyState(VK_CAPITAL);
        return (current & 1) == 1;
    }

    static public boolean getImeOpen() {
        int hWnd = user32.GetForegroundWindow();
        int hIME = imm.ImmGetDefaultIMEWnd(hWnd);
        int result = user32.SendMessageW(hIME, WM_IME_CONTROL, IMC_GETOPENSTATUS, 0);
        return result != 0;
    }

    static public boolean setImeOpen(boolean isOpen) {
        int hWnd = user32.GetForegroundWindow();
        int hIME = imm.ImmGetDefaultIMEWnd(hWnd);
        int result = user32.SendMessageW(hIME, WM_IME_CONTROL, IMC_SETOPENSTATUS, isOpen ? 1 : 0);
        return result == 0;
    }
    public static void main(String[] args) throws InterruptedException {
        boolean flag = false;
        while (true) {
            int hWND = user32.GetForegroundWindow();
            setImeOpen(!flag);
            setCapsLock(flag);
            int hIME = imm.ImmGetDefaultIMEWnd(hWND);
//            System.out.println(imm.ImmSimulateHotKey(hWND, IME_CHOTKEY_IME_NONIME_TOGGLE));
            flag = !flag;
            System.out.print(hWND);
            System.out.println(" ==> " + getImeOpen());
            System.out.println("VK_CAP => " + getCapsLock());
            sleep(3000);
        }
    }

    public interface LibUser32 extends Library {

        int GetForegroundWindow();

        int SendMessageW(int hWnd, int msg, int wParam, int lParam);

        int GetKeyState(int nVirtKey);

        int MapVirtualKeyA(int uCode, int uMapType);

        void keybd_event(byte bVk, byte bScan, long dwFlags, long dwExtraInfo);
    }

    public interface LibIMM extends Library {
        int ImmGetDefaultIMEWnd(int hWnd);
        int ImmSimulateHotKey(int hWnd, long dwHotKeyId);
    }
}


public class CRC16 {
    private byte[] mFrame;
    private int mDivisor;
    private boolean[] mDiv;
    private boolean[] mCheck;
    private byte[] mCrc;
    
    /**
     * divisor like 0x1D is used to check the remainder
     * frame is a byte array including header and counter.
     * The last byte is the CRC check.
     * @param divisor
     * @param frame
     */
    public CRC16(int divisor, byte[] frame) {
        mDivisor = divisor;
        mFrame = frame;
        initFrame();
    }
    
    private void initFrame() {
        int size = 0;
        // 8 bits at end are for CRC check
        mCheck = new boolean[(mFrame.length * 8) + 8];
        
        for (int i = 8 - 1; i >= 0; i--) {
            if (((mDivisor >> i) & 0x01) == 1) {
                size = i + 1;
                break;
            }
        }
        
        mDiv = new boolean[size];
        
        for (int i = 8 - 1; i >= 0; i--) {
            if (((mDivisor >> i) & 0x01) == 1) {
                mDiv[i] = true;
            }
        }
        
        // 5        4       3       2       1       0
        // 47-40    39-32   31-24   23-16   15-8    7-0 + 8
        for (int i = mFrame.length - 1; i >= 0; i--) {
            mCheck[8 * i - 0 + 15] = ((mFrame[i] & 0x80) == 0x80);
            mCheck[8 * i - 1 + 15] = ((mFrame[i] & 0x40) == 0x40);
            mCheck[8 * i - 2 + 15] = ((mFrame[i] & 0x20) == 0x20);
            mCheck[8 * i - 3 + 15] = ((mFrame[i] & 0x10) == 0x10);
            mCheck[8 * i - 4 + 15] = ((mFrame[i] & 0x08) == 0x08);
            mCheck[8 * i - 5 + 15] = ((mFrame[i] & 0x04) == 0x04);
            mCheck[8 * i - 6 + 15] = ((mFrame[i] & 0x02) == 0x02);
            mCheck[8 * i - 7 + 15] = ((mFrame[i] & 0x01) == 0x01);
        }
    }
    
    private void saveBinaryToByte(boolean[] bin) {
        mCrc = new byte[mFrame.length + 1];
        
        for (int i = mFrame.length - 1; i >= 0; i--) {
            mCrc[i + 1] = mFrame[i];
        }
        
        mCrc[0] = 0x00;
        mCrc[0] = (bin[7]) ? (byte) (mCrc[0] + 0x80) : mCrc[0];
        mCrc[0] = (bin[6]) ? (byte) (mCrc[0] + 0x40) : mCrc[0];
        mCrc[0] = (bin[5]) ? (byte) (mCrc[0] + 0x20) : mCrc[0];
        mCrc[0] = (bin[4]) ? (byte) (mCrc[0] + 0x10) : mCrc[0];
        mCrc[0] = (bin[3]) ? (byte) (mCrc[0] + 0x08) : mCrc[0];
        mCrc[0] = (bin[2]) ? (byte) (mCrc[0] + 0x04) : mCrc[0];
        mCrc[0] = (bin[1]) ? (byte) (mCrc[0] + 0x02) : mCrc[0];
        mCrc[0] = (bin[0]) ? (byte) (mCrc[0] + 0x01) : mCrc[0];
    }
    
    public byte[] getCrcFrame() {
        boolean[] temp = new boolean[8];
        
        for (int i = mCheck.length - 1; i > 7; i--) {
            if (mCheck[i]) {
                for (int j = 0; j < mDiv.length; j++) {
                    mCheck[i - j] = mCheck[i - j] ^ mDiv[mDiv.length - j - 1];
                }
            }
        }
        
        for (int i = 0; i < 8; i++) {
            temp[7 - i] = mCheck[7 - i];
        }
        
        saveBinaryToByte(temp);
        
        return mCrc;
    }
    
    public boolean checkFrame() {
        boolean res = true;
        
        for (int i = mCheck.length - 1; i >= 7; i--) {
            if (mCheck[i]) {
                for (int j = mDiv.length - 1; j >= 0; j--) {
                    mCheck[i - (mDiv.length - 1 - j)] = mCheck[i - (mDiv.length - 1 - j)] ^ mDiv[j];
                }
            }
        }
        
        for (int i = 0; i < mCheck.length; i++) {
            if (mCheck[i]) {
                res = false;
            }
        }
        
        return res;
    }

}

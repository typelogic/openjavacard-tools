package better.smartcard.commands;

import better.smartcard.generic.GenericCard;
import better.smartcard.generic.GenericContext;
import better.smartcard.iso.ISO7816;
import better.smartcard.iso.SW;
import better.smartcard.tool.converter.BytesConverter;
import better.smartcard.util.APDUUtil;
import better.smartcard.util.HexUtil;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

@Parameters(
        commandNames = "scan-name"
)
public class GenericScanName extends GenericCommand {

    @Parameter(
            names = "--base",
            converter = BytesConverter.class
    )
    byte[] aidBase;

    @Parameter(
            names = "--count"
    )
    int aidCount = 256;

    @Parameter(
            names = "--depth"
    )
    int aidDepth = 1;

    @Parameter(
            names = "--recurse"
    )
    int aidRecurse;

    @Parameter(
            names = "--verbose"
    )
    boolean verbose;

    public GenericScanName(GenericContext context) {
        super(context);
    }

    @Override
    protected void performOperation(GenericCard card) throws CardException {
        scanNames(card, aidBase, aidDepth, aidRecurse);
    }

    private void scanNames(GenericCard card, byte[] base, int depth, int recurse) throws CardException {
        PrintStream os = System.out;

        BigInteger bStart = new BigInteger(Arrays.copyOf(base, base.length + depth));
        BigInteger bLimit;
        if(aidDepth != 0) {
            BigInteger bDepth = BigInteger.valueOf(1 << (depth * 8));
            bLimit = bStart.add(bDepth);
        } else {
            BigInteger bCount = BigInteger.valueOf(aidCount);
            bLimit = bStart.add(bCount);
        }

        os.println("SCANNING NAMES FROM " + HexUtil.bytesToHex(bStart.toByteArray())
                + " BELOW " + HexUtil.bytesToHex(bLimit.toByteArray()));

        ArrayList<byte[]> found = new ArrayList<>();
        for(BigInteger index = bStart;
            index.compareTo(bLimit) < 0;
            index = index.add(BigInteger.ONE)) {
            byte[] name = index.toByteArray();
            if(verbose || (name[name.length - 1] == (byte)0xFF)) {
                os.println(" PROGRESS " + HexUtil.bytesToHex(name));
            }
            ResponseAPDU rapdu = performSelect(card, name, true);
            int sw = rapdu.getSW();
            if(sw == 0x9000) {
                String foundLog = "  FOUND " + HexUtil.bytesToHex(name);
                found.add(name);
                byte[] data = rapdu.getData();
                if(data.length > 0) {
                    foundLog += " DATA " + HexUtil.bytesToHex(data);
                }
                os.println(foundLog);
                card.reconnect(true);
            } else if(sw == ISO7816.SW_FILE_NOT_FOUND) {
                continue;
            } else {
                os.println("  ERROR " + HexUtil.bytesToHex(name) + " " + SW.toString(sw));
                card.reconnect(true);
            }
        }

        if(found.isEmpty()) {
            os.println("  FOUND NOTHING");
        }

        if(recurse > 0) {
            for(byte[] aid: found) {
                scanNames(card, aid, 1, recurse - 1);
            }
        }
    }

    private ResponseAPDU performSelect(GenericCard card, byte[] aid, boolean first) throws CardException {
        byte p1 = ISO7816.SELECT_P1_BY_NAME;
        byte p2 = first ? ISO7816.SELECT_P2_FIRST_OR_ONLY : ISO7816.SELECT_P2_NEXT;
        CommandAPDU scapdu = APDUUtil.buildCommand(
                ISO7816.CLA_ISO7816, ISO7816.INS_SELECT,
                p1, p2, aid);
        return card.transmit(scapdu);
    }

}
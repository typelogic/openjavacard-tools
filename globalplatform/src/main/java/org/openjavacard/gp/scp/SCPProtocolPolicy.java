/*
 * openjavacard-tools: Development tools for JavaCard
 * Copyright (C) 2018 Ingo Albrecht <copyright@promovicz.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.openjavacard.gp.scp;

import org.openjavacard.util.HexUtil;

import javax.smartcardio.CardException;

/**
 * SCP protocol selection policy
 * <p/>
 * This has been introduced because card provisioning systems
 * may be confronted with several protocol versions on different
 * cards and should want to prevent downgrade attacks.
 * <p/>
 * One such attack would be for an impostor card to indicate SCP01
 * even though the corresponding original card is supposed to
 * support SCP02. In that case the SCP02 keys would be used with
 * SCP01 and consequently with less cryptographic protection,
 * creating a possibility of malicious key derivation.
 * <p/>
 * Note that these objects can not say what level of security
 * the protocol supports because that cannot be determined
 * before knowing the protocol parameters, which this class
 * potentially polices but does not understand.
 * <p/>
 * There are no version-specific subclasses of this class
 * because we police protocols in a general manner.
 */
public final class SCPProtocolPolicy {

    /** Policy that allows any protocol to be used */
    public static final SCPProtocolPolicy PERMISSIVE = new SCPProtocolPolicy(0, 0);

    /** Policy that allows only SCP01 to be used */
    public static final SCPProtocolPolicy SCP01 = new SCPProtocolPolicy(0x01, 0);

    /** Policy that allows only SCP02 to be used */
    public static final SCPProtocolPolicy SCP02 = new SCPProtocolPolicy(0x02, 0);

    /** Policy that allows only SCP03 to be used */
    public static final SCPProtocolPolicy SCP03 = new SCPProtocolPolicy(0x03, 0);

    /**
     * SCP version to be used
     */
    public final int mScpVersion;

    /**
     * SCP parameters to be used
     */
    public final int mScpParameters;

    /**
     * Construct an SCP policy with given version and parameters
     *
     * Providing a 0 for the respective fields means ANY.
     *
     * @param scpVersion to allow
     * @param scpParameters to allow
     */
    public SCPProtocolPolicy(int scpVersion, int scpParameters) {
        mScpVersion = scpVersion;
        mScpParameters = scpParameters;
    }

    /**
     * Stringify the policy
     */
    public String toString() {
        String version = mScpVersion == 0 ? "*" : HexUtil.hex8(mScpVersion);
        String parameters = mScpParameters == 0 ? "" : ("-" + HexUtil.hex8(mScpParameters));
        return "SCP" + version + parameters;
    }

    /** @return true if the given SCP version is allowed */
    public boolean isVersionAllowed(int version) {
        return mScpVersion == 0 || mScpVersion == version;
    }

    /** @return true if the given SCP parameters are allowed */
    public boolean isParametersAllowed(int parameters) {
        return mScpParameters == 0 || mScpParameters == parameters;
    }

    /** @return true if the given SCP protocol is allowed */
    public boolean isProtocolAllowed(int version, int parameters) {
        return isVersionAllowed(version) && isParametersAllowed(parameters);
    }

    /** @return true if the given SCP protocol is allowed */
    public boolean isProtocolAllowed(SCPParameters protocol) {
        return isVersionAllowed(protocol.scpVersion) && isParametersAllowed(protocol.scpParameters);
    }

    /**
     * Check the given SCP protocol against the policy
     *
     * Variant used for the protocol implementation.
     *
     * @throws CardException if the protocol is denied use
     */
    public void checkProtocol(SCPParameters p) throws CardException {
        checkProtocol(p.scpVersion, p.scpParameters);
    }

    /**
     * Check the given SCP protocol against the policy
     *
     * Variant used for parameter validation.
     *
     * @throws CardException if the protocol is denied use
     */
    public void checkProtocol(int scpProtocol, int scpParameters) throws CardException {
        if (!isProtocolAllowed(scpProtocol, scpParameters)) {
            throw new CardException("Protocol SCP" + HexUtil.hex8(scpProtocol)
                    + "-" + HexUtil.hex8(scpParameters)
                    + " denied by policy");
        }
    }

}

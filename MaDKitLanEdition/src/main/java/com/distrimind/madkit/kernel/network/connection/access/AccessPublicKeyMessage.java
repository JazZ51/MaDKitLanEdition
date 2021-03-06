/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel.network.connection.access;

import java.io.IOException;
import com.distrimind.util.crypto.ASymmetricPublicKey;
import com.distrimind.util.data_buffers.WrappedData;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("unused")
class AccessPublicKeyMessage extends AccessMessage {

	private byte[] public_key_bytes;
	private transient WrappedData distant_public_key;
	private boolean otherCanTakeLoginInitiative;
	private static final int MAX_DISTANT_PUBLIC_KEY_LENGTH=16392; 
	
	@SuppressWarnings("unused")
	AccessPublicKeyMessage()
	{
		
	}
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		public_key_bytes=in.readBytesArray(false, MAX_DISTANT_PUBLIC_KEY_LENGTH);
		otherCanTakeLoginInitiative=in.readBoolean();
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeBytesArray(public_key_bytes, false, MAX_DISTANT_PUBLIC_KEY_LENGTH);
		oos.writeBoolean(otherCanTakeLoginInitiative);
	}
	
	public AccessPublicKeyMessage(ASymmetricPublicKey _public_key, ASymmetricPublicKey _distant_public_key,
			boolean otherCanTakeLoginInitiative) {
		public_key_bytes = _public_key.encode().getBytes();
		distant_public_key = _distant_public_key == null ? null : _distant_public_key.encode();
		this.otherCanTakeLoginInitiative = otherCanTakeLoginInitiative;
	}

	public boolean isOtherCanTakeLoginInitiative() {
		return otherCanTakeLoginInitiative;
	}

	public byte[] getEncodedPublicKey() {
		return public_key_bytes;
	}

	@Override
	public void corrupt() {
		if (distant_public_key != null)
			public_key_bytes = distant_public_key.getBytes();
	}


	@Override
	public boolean checkDifferedMessages() {
		return false;
	}



}

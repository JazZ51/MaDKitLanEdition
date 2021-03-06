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
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.kernel.network.Block;
import com.distrimind.madkit.kernel.network.RealTimeTransferStat;
import com.distrimind.madkit.util.NetworkMessage;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Message received when a big data transfer is requested.
 * 
 * By calling the function
 * {@link BigDataPropositionMessage#acceptTransfer(RandomOutputStream)},
 * the transfer will be able to begin.
 * 
 * By calling the function {@link BigDataPropositionMessage#denyTransfer()}, the
 * transfer will rejected.
 * 
 * @author Jason Mahdjoub
 * @version 1.3
 * @since MadkitLanEdition 1.0
 * 
 * @see AbstractAgent#sendBigDataWithRole(AgentAddress, RandomInputStream, long, long, SecureExternalizable, MessageDigestType, String, boolean)
 * @see BigDataResultMessage
 */
@SuppressWarnings({"unused"})
public final class BigDataPropositionMessage extends Message implements NetworkMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1785811403975318464L;

	protected static final int maxBufferSize = 1024 * 1024;

	protected final transient RandomInputStream inputStream;
	protected transient RandomOutputStream outputStream = null;
	private transient RealTimeTransferStat stat = null;
	protected long pos;
	protected long length;
	private SecureExternalizable attachedData;
	private byte[] data;
	private boolean isLocal;
	protected int idPacket;
	protected long timeUTC;
	private MessageDigestType messageDigestType;
	private boolean excludedFromEncryption;
	@SuppressWarnings("unused")
	private BigDataPropositionMessage()
	{
		inputStream=null;
	}
	@Override
	public int getInternalSerializedSize() {
		return super.getInternalSerializedSizeImpl()+37+(attachedData==null?0:attachedData.getInternalSerializedSize())+(data==null?0:data.length)+(messageDigestType==null?0:messageDigestType.name().length()*2);
	}
	
	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);
		pos=in.readLong();
		length=in.readLong();
		attachedData=in.readObject(true, SecureExternalizable.class);

		data=in.readBytesArray(true, Block.BLOCK_SIZE_LIMIT);
		isLocal=in.readBoolean();
		idPacket=in.readInt();
		timeUTC=in.readLong();
		String s=in.readString(true, 1000);
		if (s==null)
			messageDigestType=null;
		else 
		{	
			messageDigestType=MessageDigestType.valueOf(s);
		}
		
		excludedFromEncryption=in.readBoolean();
			
		
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException{
		super.writeExternal(oos);
		oos.writeLong(pos);
		oos.writeLong(length);
		
		oos.writeObject(attachedData, true);
		
		oos.writeBytesArray(data, true, Block.BLOCK_SIZE_LIMIT);
		oos.writeBoolean(isLocal);
		oos.writeInt(idPacket);
		oos.writeLong(timeUTC);
		oos.writeString(messageDigestType==null?null:messageDigestType.name(), true, 1000);
		
		oos.writeBoolean(excludedFromEncryption);
	}


	
	BigDataPropositionMessage(RandomInputStream stream, long pos, long length, SecureExternalizable attachedData, boolean local,
							  int maxBufferSize, RealTimeTransferStat stat, MessageDigestType messageDigestType, boolean excludedFromEncryption) throws IOException {
		if (stream == null)
			throw new NullPointerException("stream");
		if (pos >= stream.length())
			throw new IllegalArgumentException("pos must be lower than stream.length()");
		if (length > stream.length() - pos)
			throw new IllegalArgumentException("length cannot be greater than stream.length()-pos");
		if (maxBufferSize>Block.BLOCK_SIZE_LIMIT)
			throw new IllegalArgumentException();
		this.pos = pos;
		this.length = length;
		this.attachedData = attachedData;
		if (local || maxBufferSize < stream.length()) {
			this.inputStream = stream;
			this.data = null;
		} else {
			this.inputStream = null;
			this.data = new byte[(int) stream.length()];
			if (stream.read(this.data)!=data.length)
				throw new IOException();
		}
		this.isLocal = local;
		this.stat = stat;
		timeUTC = System.currentTimeMillis();
		this.messageDigestType = messageDigestType;
		this.excludedFromEncryption=excludedFromEncryption;

	}


	public boolean bigDataExcludedFromEncryption()
	{
		return excludedFromEncryption;
	}
	
	/**
	 * Gets the user customized data attached to this big data transfer proposition
	 * 
	 * @return the user customized data attached to this big data transfer
	 *         proposition, or null
	 */
	public SecureExternalizable getAttachedData() {
		return attachedData;
	}

	/**
	 * Tells if the transfer was done locally with the same MadkitKernel
	 * 
	 * @return true if the transfer was done locally with the same MadkitKernel,
	 *         false else.
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * 
	 * @return the start position of the source stream
	 */
	public long getStartStreamPosition() {
		return pos;
	}

	/**
	 * 
	 * @return the length in bytes of the data to transfer.
	 */
	public long getTransferLength() {
		return length;
	}

	/**
	 * Gets statistics in bytes per seconds related to the concerned big data
	 * transfer
	 * 
	 * @return statistics in bytes per seconds related to the concerned big data
	 *         transfer
	 */
	public RealTimeTransferStat getStatistics() {
		if (stat == null) {
			final AbstractAgent receiver = getReceiver().getAgent();
			stat = new RealTimeTransferStat(receiver.getMadkitConfig().networkProperties.bigDataStatDurationMean,
					receiver.getMadkitConfig().networkProperties.bigDataStatDurationMean / 10);
		}
		return stat;

	}

	/**
	 * Accept the transfer A message {@link BigDataResultMessage} is sent in return
	 * to the agent asking for the transfer, to inform him of the transfer result
	 * (see {@link BigDataResultMessage.Type}).
	 * 
	 * @param outputStream
	 *            the output stream to use during the transfer
	 * @throws InterruptedException if the current thread is interrupted
	 */
	public void acceptTransfer(final RandomOutputStream outputStream) throws InterruptedException {
		if (outputStream == null)
			throw new NullPointerException("outputStream");
		final AbstractAgent receiver = getReceiver().getAgent();
		this.outputStream = outputStream;
		if (isLocal()) {
			try {

				receiver.scheduleTask(new Task<>((Callable<Void>) () -> {
					long remaining = length;
					try {
						byte[] buffer = new byte[(int) Math.min(length, maxBufferSize)];
						inputStream.seek(pos);
						outputStream.setLength(length);
						while (remaining > 0) {
							int s = (int) Math.min(buffer.length, remaining);
							if (inputStream.read(buffer, 0, s)!=s)
								throw new IOException();
							outputStream.write(buffer, 0, s);
							remaining -= s;
						}
					} catch(MessageExternalizationException e)
					{
						dataCorrupted(length - remaining, e);
					}
					catch (Exception e) {
						sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_PARTIALLY_TRANSFERRED,
								length - remaining);
						throw e;
					}
					sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, length);
					return null;
				})).waitTaskFinished();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

		} else {
			if (data != null) {
				try {
					outputStream.write(data);
				} catch(MessageExternalizationException e)
				{
					dataCorrupted(0, e);
				}catch (IOException e) {
					sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_PARTIALLY_TRANSFERRED, 0);
				}
				sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, length);
			} else {

				receiver.getKernel().acceptDistantBigDataTransfer(receiver, this);
			}
		}
	}

	/**
	 * 
	 * @return the message digest type used for check the validity of the transferred
	 *         data
	 */
	public MessageDigestType getMessageDigestType() {
		return messageDigestType;
	}

	/**
	 * Reject the transfer A message {@link BigDataResultMessage} is sent in return
	 * to the agent asking for the transfer, to inform him of the transfer result
	 * (see {@link BigDataResultMessage.Type}).
	 * 
	 * @throws InterruptedException if the current thread is interrupted
	 */
	public void denyTransfer() throws InterruptedException {
		final AbstractAgent receiver = getReceiver().getAgent();
		try {
			receiver.scheduleTask(new Task<>((Callable<Void>) () -> {
				if (receiver.isAlive()) {
					Message m = new BigDataResultMessage(BigDataResultMessage.Type.BIG_DATA_TRANSFER_DENIED, 0,
							idPacket, System.currentTimeMillis() - timeUTC);
					m.setIDFrom(BigDataPropositionMessage.this);
					receiver.sendMessage(getSender(), m);
				}
				return null;
			})).waitTaskFinished();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

	void connectionLost(long dataTransferred) {
		sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_PARTIALLY_TRANSFERRED, dataTransferred);
	}

	void dataCorrupted(long dataTransferred, MessageExternalizationException e) {
		sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_CORRUPTED, dataTransferred);
		final AbstractAgent receiver = getReceiver().getAgent();
		KernelAddress senderKernelAddress=getSender().getKernelAddress();
		if (e!=null && !senderKernelAddress.equals(receiver.getKernelAddress())) {
			receiver.anomalyDetectedWithOneDistantKernel(e.getIntegrity().equals(Integrity.FAIL_AND_CANDIDATE_TO_BAN), senderKernelAddress, e.getMessage());
		}
	}

	void transferCompleted(long dataTransferred) {
		sendBidirectionalReply(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, dataTransferred);
	}

	protected void sendBidirectionalReply(final BigDataResultMessage.Type type, final long length) {
		final AbstractAgent receiver = getReceiver().getAgent();

		receiver.scheduleTask(new Task<>((Callable<Void>) () -> {
			Message m = new BigDataResultMessage(type, length, idPacket, System.currentTimeMillis() - timeUTC);
			m.setIDFrom(BigDataPropositionMessage.this);
			receiver.sendMessage(getSender(), m);
			return null;
		}));

		Message m = new BigDataResultMessage(type, length, idPacket, System.currentTimeMillis() - timeUTC);
		m.setReceiver(getReceiver());
		m.setSender(getSender());
		m.setIDFrom(BigDataPropositionMessage.this);
		receiver.receiveMessage(m);
	}

	RandomInputStream getInputStream() {
		return inputStream;
	}

	void setIDPacket(int idPacket) {
		this.idPacket = idPacket;
	}

	int getIDPacket() {
		return idPacket;
	}

	RandomOutputStream getOutputStream() {
		return outputStream;
	}
	
	
}

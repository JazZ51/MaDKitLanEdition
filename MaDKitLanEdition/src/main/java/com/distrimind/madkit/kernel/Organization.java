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

import com.distrimind.madkit.i18n.ErrorMessages;
import com.distrimind.madkit.kernel.network.connection.access.GroupsRoles;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static com.distrimind.madkit.i18n.I18nUtilities.getCGRString;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 3.0
 * @since MadKitLanEdition 1.0
 * @version 5.1
 */
@SuppressWarnings("SameParameterValue")
final class Organization extends ConcurrentHashMap<Group, InternalGroup> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1547623313555380703L;
	private final Logger logger;
	private final String communityName;
	private final transient MadkitKernel myKernel;

	/**
	 * @return the myKernel
	 */
	final MadkitKernel getMyKernel() {
		return myKernel;
	}


	Organization(final String string, final MadkitKernel madkitKernel) {
		if (string == null)
			throw new NullPointerException(ErrorMessages.C_NULL.toString());
		communityName = string;
		myKernel = madkitKernel;
		// logger = madkitKernel.getLogger();
		logger = null;
	}

	String getName() {
		return communityName;
	}


	boolean addGroup(final AbstractAgent creator, Group group, boolean manually_created) {

		synchronized (this) {
			if (!group.getCommunity().equals(communityName))
				throw new IllegalAccessError();
			InternalGroup g = get(group);
			if (g == null) {// There was no such group
				g = new InternalGroup(group, creator, this, manually_created);

				put(group, g);
				group.setMadKitCreated(myKernel.getKernelAddress(), true);
				if (logger != null)
					logger.fine(getCGRString(group) + "created by " + creator.getName() + "\n");

				return true;
			}
			if (logger != null)
				logger.finer(group + "already exists: Creation aborted" + "\n");
			return false;
		}
	}


	void removeGroup(final Group group) {
		synchronized (this) {
			if (logger != null)
				logger.finer("Removing" + getCGRString(group));
			if (remove(group) != null) {
				group.setMadKitCreated(this.myKernel.getKernelAddress(), false);
				checkEmptyness();
			}
		}
	}

	private void checkEmptyness() {
		if (isEmpty()) {
			if (logger != null)
				logger.finer("Removing" + getCGRString(communityName));
			myKernel.removeCommunity(communityName);
		}
	}


	ArrayList<Group> removeAgentFromAllGroups(final AbstractAgent theAgent, boolean manually_requested) {
		final ArrayList<Group> groups = new ArrayList<>();
		for (final Iterator<Map.Entry<Group, InternalGroup>> e = this.entrySet().iterator(); e.hasNext();) {
			final Map.Entry<Group, InternalGroup> entry = e.next();
			final InternalGroup g = entry.getValue();
			if (g.leaveGroup(theAgent, manually_requested) != null) {// at least present in one group
				if (g.isDistributed()) {
					groups.add(entry.getKey());
				}
				if (g.isEmpty())
					e.remove();
			}
		}
		return groups;

	}

	void removeDistantKernelAddressForAllGroups(KernelAddress ka)
	{
		for (final Iterator<Map.Entry<Group, InternalGroup>> e = this.entrySet().iterator(); e.hasNext();) {
			final Map.Entry<Group, InternalGroup> entry = e.next();
			final InternalGroup g = entry.getValue();
			g.removeDistantKernelAddressForAllRoles(ka);
			if (g.isEmpty())
				e.remove();
		}

	}

	void updateAcceptedDistantGroupsGivenToDistantPeer(KernelAddress ka, ListGroupsRoles generalAcceptedGroup)
	{
		for (final Iterator<Map.Entry<Group, InternalGroup>> e = this.entrySet().iterator(); e.hasNext();) {
			final Map.Entry<Group, InternalGroup> entry = e.next();
			final InternalGroup g = entry.getValue();

			if (!generalAcceptedGroup.getGroups().includes(ka, g.getGroup()))
				g.removeDistantKernelAddressForAllRoles(ka);
			else
				g.removeDistantKernelAddressForAllRolesThatDoesNotAcceptDistantRoles(ka, generalAcceptedGroup.getGroupsRoles(g.getGroup()));
			if (g.isEmpty())
				e.remove();
		}
	}


	Map<Group, Map<String, Collection<AgentAddress>>> getOrgMap(boolean global) {
		Map<Group, Map<String, Collection<AgentAddress>>> export = new TreeMap<>();
		for (Map.Entry<Group, InternalGroup> org : entrySet()) {
			if (global || org.getValue().isDistributed()) {
				export.put(org.getKey(), org.getValue().getGroupMap());
			}
		}
		return export;
	}

	Map<Group, Map<String, Collection<AgentAddress>>> getOrgMap(KernelAddress localKernelAddress, List<Group> concerned_groups, ListGroupsRoles distantAcceptedGroups, boolean global) {
		Map<Group, Map<String, Collection<AgentAddress>>> export = new TreeMap<>();
		for (Map.Entry<Group, InternalGroup> org : entrySet()) {
			if (global || org.getValue().isDistributed()) {
				if (concerned_groups.contains(org.getValue().getGroup())) {
					GroupsRoles gr = distantAcceptedGroups.getGroupsRoles(org.getValue().getGroup());
					if (gr != null) {
						String[] roles = gr.getDistantAcceptedRoles();
						Map<String, Collection<AgentAddress>> m;
						if (roles == null || roles.length == 0)
							m = org.getValue().getGroupMap();
						else
							m = org.getValue().getGroupMap(localKernelAddress, distantAcceptedGroups);
						if (!m.isEmpty()) {
							export.put(org.getKey(), m);
						}
					}
				}
			}
		}
		return export;
	}


	void importDistantOrg(Map<Group, Map<String, Collection<AgentAddress>>> map, MadkitKernel madkitKernel) {
		for (Group groupName : map.keySet()) {
			if (groupName.isDistributed()) {
				Map<String, Collection<AgentAddress>> groupContent=map.get(groupName);
				boolean empty=true;
				for (Collection<AgentAddress> c : groupContent.values())
				{
					if (c.size() > 0 && c.iterator().next() != null) {
						empty=false;
						break;
					}
				}
				if (empty)
					continue;

				InternalGroup group = get(groupName);
				if (group == null) {
					AgentAddress manager;
					try {
						manager = groupContent.get(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)
								.iterator().next();
					} catch (NullPointerException e) {// TODO a clean protocol to get the groupManager
						manager = groupContent.values().iterator().next().iterator().next();
					}
					group = new InternalGroup(groupName, manager, this, false);
					put(groupName, group);
				}
				if (group.isDistributed())
					group.importDistantOrg(groupContent, madkitKernel);
			}



		}
	}

	/*void removeAgentsFromDistantKernel(KernelAddress kernelAddress2, MadkitKernel madkitKernel) {
		for (InternalGroup group : values()) {
			if (group.isDistributed()) {
				group.removeAgentsFromDistantKernel(kernelAddress2, madkitKernel);
			}
		}
	}*/

	void removeDistantGroup(KernelAddress distantKernelAddress, Group distantGroup, MadkitKernel madkitKernel) {
		for (InternalGroup group : values()) {
			if (group.isDistributed() && group.getGroup().equals(distantGroup)) {
				group.removeAgentsFromDistantKernel(distantKernelAddress, madkitKernel);
			}
		}
	}

	Logger getLogger() {
		return logger;
	}

	void destroy() {
		for (final InternalGroup g : values()) {
			g.destroy();
		}
		myKernel.removeCommunity(communityName);
	}

}
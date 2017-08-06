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
package com.distrimind.madkit.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.Activator;
import com.distrimind.madkit.kernel.Agent;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.testing.util.agent.NormalLife;

/**
 * Has to be outside of madkit.kernel for really testing visibility
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.13
 * @version 0.9
 * 
 */
public class ActivatorTest {

	Activator<AbstractAgent> a;
	Agent agt;

	@Before
	public void setUp() throws Exception {
		a = new EmptyActivator(new Group("t", "t"), "t");
		agt = new Agent();
	}

	@Test
	public void testToString() {
		System.err.println(a);
	}

	@Test
	public void testFindMethodOn() {
		try {
			Method m;
			m = Activator.findMethodOn(AbstractAgent.class, "activate");
			System.err.println(m);
			m = Activator.findMethodOn(NormalLife.class, "liveCycle");// protected
			System.err.println(m);
			m = Activator.findMethodOn(NormalLife.class, "privateMethod");// private
			System.err.println(m);
			m.invoke(new NormalLife());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			fail("Oo");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail("Oo");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			fail("Oo");
		} catch (InvocationTargetException e) {
		}
	}

	@Test
	public void testIsMulticoreModeOn() {
		assertFalse(a.isMulticoreModeOn());
		a.useMulticore(1);
		assertFalse(a.isMulticoreModeOn());
		a.useMulticore(2);
		assertTrue(a.isMulticoreModeOn());
		a.useMulticore(1);
		assertFalse(a.isMulticoreModeOn());
		a.useMulticore(-1);
		assertFalse(a.isMulticoreModeOn());
	}

	@Test
	public void testSetMulticore() {
		a.useMulticore(-1);
		assertEquals(1, a.nbOfParallelTasks());
		a.useMulticore(2);
		assertEquals(2, a.nbOfParallelTasks());
	}

}
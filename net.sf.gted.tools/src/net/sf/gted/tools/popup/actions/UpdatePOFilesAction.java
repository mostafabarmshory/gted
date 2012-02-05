/*
 gted - gted.sourceforge.net
 Copyright (C) 2007 by Simon Martinelli, Gampelen, Switzerland

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.gted.tools.popup.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.gted.tools.properties.ProjectPropertyPage;
import net.sf.gted.tools.util.ProcessHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The Class UpdatePOFilesAction.
 * 
 * @author $Author: simas_ch $
 * @version $Revision: 1.19 $, $Date: 2009/04/28 10:51:43 $
 */
public class UpdatePOFilesAction implements IObjectActionDelegate {

	private IProject project;

	/**
	 * Constructor for Action1.
	 */
	public UpdatePOFilesAction() {
		super();
	}

	/**
	 * Sets the active part.
	 * 
	 * @param action
	 *            the action
	 * @param targetPart
	 *            the target part
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
	}

	/**
	 * Run.
	 * 
	 * @param action
	 *            the action
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(final IAction action) {

		List<String> command = new ArrayList<String>();
		String error = null;
		try {
			// 1. xgettext
			command = this.prepareXgettext();
			error = ProcessHelper.executeCommand(this.project, true, command);

			if (error == null) {
				// 2. msgmerge
				error = this.makeMsgmerge();
			}
			this.project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (final Exception e) {
			e.printStackTrace();
			error = e.getMessage();
		}
		if (error != null) {
			final Shell shell = new Shell();
			MessageDialog.openInformation(shell, "Update PO Files", error);
		}
	}

	/**
	 * @param command
	 * @return
	 * @throws CoreException
	 */
	private List<String> prepareXgettext() throws CoreException {

		final String extensions = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_EXTENSION_PROPERTY));

		final String keywords = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_KEYWORD_PROPERTY));

		final String fromcode = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_FROMCODE_PROPERTY));

		final String domainname = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_DOMAIN_NAME_PROPERTY));

		final String outputfolder = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_OUTPUT_PROPERTY));

		final String language = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.LANGUAGE_PROPERTY));

		List<String> command = new ArrayList<String>();
		command.add("xgettext");

		if (language != null && !language.equals("")) {
			command.add("-L" + language);
		}

		if (fromcode != null && !fromcode.equals("")) {
			command.add("--from-code=" + fromcode);
		}

		if (keywords != null && !keywords.equals("")) {
			List<String> keywordList = this.extractStrings(keywords);
			for (String keyword : keywordList) {
				command.add("-k" + keyword);
			}
		}

		command.add("-o" + outputfolder + "/" + domainname + ".pot");

		final List<String> extList = this.extractStrings(extensions);

		this.getFiles(this.project.members(), extList, command);

		return command;
	}

	/**
	 * @param extensions
	 * @return
	 */
	private List<String> extractStrings(final String strings) {
		final List<String> extList = new ArrayList<String>();

		final String[] stringList = strings.split(",");
		for (final String string : stringList) {
			extList.add(string.trim());
		}

		return extList;
	}

	/**
	 * @param string
	 * @return
	 * @throws CoreException
	 */
	private List<String> prepareMsgmerge(final String language)
			throws CoreException {
		final String outputfolder = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_OUTPUT_PROPERTY));

		final String domainname = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_DOMAIN_NAME_PROPERTY));

		List<String> command = new ArrayList<String>();
		command.add("msgmerge");
		command.add("-U");
		command.add(outputfolder + "/" + language + "/" + domainname + ".po");
		command.add(outputfolder + "/" + domainname + ".pot");

		return command;
	}

	/**
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private String makeMsgmerge() throws CoreException, IOException,
			InterruptedException {
		final String outputfolder = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_OUTPUT_PROPERTY));
		final String domainname = this.project
				.getPersistentProperty(new QualifiedName(
						ProjectPropertyPage.QUALIFIER,
						ProjectPropertyPage.XGETTEXT_DOMAIN_NAME_PROPERTY));

		final IFolder folder = this.project.getFolder(outputfolder);

		for (final IResource resource : folder.members()) {
			if (resource instanceof IFolder) {
				final IFolder trFolder = (IFolder) resource;
				final IFile file = trFolder.getFile(domainname + ".po");
				if (file.exists()) {
					final List<String> command = this.prepareMsgmerge(resource
							.getName());
					final String error = ProcessHelper.executeCommand(
							this.project, true, command);
					if (error == null || error.startsWith(".")) {
						// do nothing it's ok
					} else {
						return error;
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param resources
	 * @return
	 */
	private void getFiles(final IResource[] resources,
			final List<String> extList, List<String> command) {

		for (final IResource resource : resources) {
			if (resource instanceof IFile) {
				final IFile file = (IFile) resource;
				if (extList.contains(file.getFileExtension())) {
					command.add(file.getProjectRelativePath().toString());
				}
			} else if (resource instanceof IFolder) {
				final IFolder folder = (IFolder) resource;
				try {
					this.getFiles(folder.members(), extList, command);
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Selection changed.
	 * 
	 * @param action
	 *            the action
	 * @param selection
	 *            the selection
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(final IAction action,
			final ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof IProject) {
				this.project = (IProject) structuredSelection.getFirstElement();
			}
		}
	}

}
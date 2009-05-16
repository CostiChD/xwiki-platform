/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TabPanel;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.WysiwygServiceAsync;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Editor tab events handler.
 * 
 * @version $Id: $
 */
public class WysiwygEditorListener implements TabListener
{
    /**
     * Switch from Wiki to WYSIWYG editor.
     */
    private class SwitchToWysiwygCallback implements AsyncCallback<String>
    {
        /**
         * WysiwygEditor instance.
         */
        private WysiwygEditor editor;

        /**
         * Constructor.
         * 
         * @param editor WysiwygEditor instance.
         */
        SwitchToWysiwygCallback(WysiwygEditor editor)
        {
            this.editor = editor;
        }

        /**
         * {@inheritDoc}
         */
        public void onSuccess(String result)
        {
            editor.setLoading(false);
            // Enable the rich text area in order to be able to submit its content.
            editor.getRichTextEditor().getTextArea().getCommandManager().execute(ENABLE, true);
            // Reset the content of the rich text area.
            editor.getRichTextEditor().getTextArea().getCommandManager().execute(new Command("reset"), result);
            // Store the initial value of the rich text area in case it is submitted without gaining focus.
            editor.getRichTextEditor().getTextArea().getCommandManager().execute(SUBMIT, true);
        }

        /**
         * {@inheritDoc}
         */
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
            editor.setLoading(false);
        }
    }

    /**
     * Switch from WYSIWYG to Wiki editor.
     */
    private class SwitchToWikiCallback implements AsyncCallback<String>
    {
        /**
         * WysiwygEditor instance.
         */
        private WysiwygEditor editor;

        /**
         * Constructor.
         * 
         * @param editor WysiwygEditor instance.
         */
        SwitchToWikiCallback(WysiwygEditor editor)
        {
            this.editor = editor;
        }

        /**
         * {@inheritDoc}
         */
        public void onSuccess(String result)
        {
            editor.setLoading(false);
            // Disable the rich text area to avoid submitting its content.
            editor.getRichTextEditor().getTextArea().getCommandManager().execute(WysiwygEditorListener.ENABLE, false);
            editor.getPlainTextEditor().getTextArea().setText(result);
        }

        /**
         * {@inheritDoc}
         */
        public void onFailure(Throwable caught)
        {
            Window.alert(caught.getMessage());
            editor.setLoading(false);
        }
    }

    /**
     * The command used to store the value of the rich text area before submitting the including form.
     */
    protected static final Command SUBMIT = new Command("submit");

    /**
     * Disable command.
     */
    protected static final Command ENABLE = new Command("enable");

    /**
     * Field describing the syntax used in the editor configuration.
     */
    protected static final String SYNTAX_CONFIG_PARAMETER = "syntax";

    /**
     * WysiwygEditor instance.
     */
    private final WysiwygEditor editor;

    /**
     * Constructor.
     * 
     * @param editor WysiwygEditor instance.
     */
    WysiwygEditorListener(WysiwygEditor editor)
    {
        this.editor = editor;
    }

    /**
     * {@inheritDoc}
     */
    public boolean onBeforeTabSelected(SourcesTabEvents sender, int index)
    {
        TabPanel tabPanel = (TabPanel) sender;
        if (tabPanel.getTabBar().getSelectedTab() == index) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void onTabSelected(SourcesTabEvents sender, int index)
    {
        editor.setLoading(true);
        WysiwygServiceAsync wysiwygService = WysiwygService.Singleton.getInstance();
        // We test if the RTE textarea is disabled to be sure that the editor is not already being switched.
        if (index == WysiwygEditor.WYSIWYG_TAB_INDEX && !editor.getRichTextEditor().getTextArea().isEnabled()) {
            wysiwygService.toHTML(editor.getPlainTextEditor().getTextArea().getText(), editor.getConfig().getParameter(
                SYNTAX_CONFIG_PARAMETER, WysiwygEditor.DEFAULT_SYNTAX), new SwitchToWysiwygCallback(editor));
        } else {
            // We test if the RTE textarea is enabled to be sure that the editor is not already being switched.  
            if (index == WysiwygEditor.WIKI_TAB_INDEX && editor.getRichTextEditor().getTextArea().isEnabled()) {
                // Notify the plug-ins that the content of the rich text area is about to be submitted.
                editor.getRichTextEditor().getTextArea().getCommandManager().execute(SUBMIT);
                // At this point we should have the HTML, adjusted by plug-ins, in the hidden plain text area.
                // Make the request to convert the HTML to Wiki syntax.
                wysiwygService.fromHTML(editor.getPlainTextEditor().getTextArea().getText(), 
                    editor.getConfig().getParameter(SYNTAX_CONFIG_PARAMETER, WysiwygEditor.DEFAULT_SYNTAX), 
                    new SwitchToWikiCallback(editor));
            }
        }
    }
}

package org.asciidoc.intellij.template;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;

public class AsciiDocTemplatesProvider implements DefaultLiveTemplatesProvider {
    @Override
    public String[] getDefaultLiveTemplateFiles() {
        return new String[] {"liveTemplates/AsciiDocLiveTemplates"};
    }

    @Override
    public String[] getHiddenLiveTemplateFiles() {
        return new String[0];
    }
}

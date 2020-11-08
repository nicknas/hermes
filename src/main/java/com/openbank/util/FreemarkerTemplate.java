package com.openbank.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringReader;

/**
 * An example of a Freemarker template engine, used to generate test data for REST API testing.
 */
public class FreemarkerTemplate {

    private Configuration configuration = null;

    private Configuration getConfiguration() {
        if (configuration == null) {
            configuration = new Configuration(Configuration.VERSION_2_3_28);
            configuration.setDefaultEncoding("UTF-8");
            configuration.setClassForTemplateLoading(FreemarkerTemplate.class, "/");
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        }

        return configuration;
    }

    /**
     * The template string -> Templte
     * @param templateStr
     * @return
     */
    public Template getTemplate(String templateStr) {
        try {
            Template t = new Template("templateStr", new StringReader(templateStr),
                    getConfiguration());
            return t;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load template", e);
        }
    }

}

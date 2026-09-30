package co.edu.uptc.universiry.platform.i18n.infrastructure;

import co.edu.uptc.universiry.platform.i18n.application.MessageCatalog;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SpringMessageCatalog implements MessageCatalog {

    private final MessageSource messageSource;

    public SpringMessageCatalog(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}

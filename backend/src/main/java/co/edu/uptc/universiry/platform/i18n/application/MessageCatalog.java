package co.edu.uptc.universiry.platform.i18n.application;

import java.util.Locale;

public interface MessageCatalog {

    String message(String key, Locale locale);
}

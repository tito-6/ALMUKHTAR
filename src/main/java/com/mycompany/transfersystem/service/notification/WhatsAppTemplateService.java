package com.mycompany.transfersystem.service.notification;

import com.mycompany.transfersystem.entity.NotificationTemplate;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.repository.NotificationTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsAppTemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final NotificationTemplateRepository templateRepository;

    public WhatsAppTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public Optional<NotificationTemplate> findTemplate(String templateKey, String languageCode) {
        String lang = languageCode == null ? "AR" : languageCode.toUpperCase();
        Optional<NotificationTemplate> exact = templateRepository.findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
                templateKey, lang, NotificationChannel.WHATSAPP);
        if (exact.isPresent()) {
            return exact;
        }
        Optional<NotificationTemplate> ar = templateRepository.findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
                templateKey, "AR", NotificationChannel.WHATSAPP);
        if (ar.isPresent()) {
            return ar;
        }
        return templateRepository.findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
                templateKey, "EN", NotificationChannel.WHATSAPP);
    }

    public String renderBody(String templateKey, String languageCode, Map<String, String> variables) {
        NotificationTemplate tpl = findTemplate(templateKey, languageCode)
                .orElseThrow(() -> new IllegalStateException("Missing WhatsApp template: " + templateKey));
        return render(tpl.getBodyTemplate(), variables);
    }

    public String render(String bodyTemplate, Map<String, String> variables) {
        Matcher m = PLACEHOLDER.matcher(bodyTemplate);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String value = variables.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public List<String> orderedPlaceholderKeys(String bodyTemplate) {
        Matcher m = PLACEHOLDER.matcher(bodyTemplate);
        List<String> keys = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            String k = m.group(1);
            if (seen.add(k)) {
                keys.add(k);
            }
        }
        return keys;
    }
}

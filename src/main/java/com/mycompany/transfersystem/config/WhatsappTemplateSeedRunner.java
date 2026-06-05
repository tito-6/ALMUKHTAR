package com.mycompany.transfersystem.config;

import com.mycompany.transfersystem.entity.NotificationTemplate;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import com.mycompany.transfersystem.entity.enums.NotificationEventType;
import com.mycompany.transfersystem.entity.enums.NotificationMessageCategory;
import com.mycompany.transfersystem.repository.NotificationTemplateRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Seeds WhatsApp templates when the table is empty or rows are missing (Flyway may be disabled in H2 dev).
 */
@Component
@Order(5000)
public class WhatsappTemplateSeedRunner implements ApplicationRunner {

    private final NotificationTemplateRepository notificationTemplateRepository;

    public WhatsappTemplateSeedRunner(NotificationTemplateRepository notificationTemplateRepository) {
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (NotificationEventType eventType : NotificationEventType.values()) {
            String key = eventType.name();
            NotificationMessageCategory cat = eventType.getMessageCategory();
            upsert(key, "EN", englishBody(eventType), cat);
            upsert(key, "AR", arabicBody(eventType), cat);
            upsert(key, "TR", "[TR] " + englishBody(eventType), cat);
            upsert(key, "FR", "[FR] " + englishBody(eventType), cat);
        }
        seedFinancialDotKeys();
    }

    private void seedFinancialDotKeys() {
        for (String key : FINANCIAL_DOT_KEYS) {
            upsert(key, "EN", "{{headline}}\n\n{{detail}}", NotificationMessageCategory.OPERATIONAL);
            upsert(key, "AR", "{{headline}}\n\n{{detail}}", NotificationMessageCategory.OPERATIONAL);
            upsert(key, "TR", "[TR] {{headline}}\n\n{{detail}}", NotificationMessageCategory.OPERATIONAL);
            upsert(key, "FR", "[FR] {{headline}}\n\n{{detail}}", NotificationMessageCategory.OPERATIONAL);
        }
    }

    private static final String[] FINANCIAL_DOT_KEYS = new String[]{
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_CREATED_SENDER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_CREATED_RECEIVER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_CREATED_BRANCH_ACTION,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_CREATED_PLATFORM_SUMMARY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_COMPLETED_SENDER_RECEIPT,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_COMPLETED_RECEIVER_PAYOUT,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_COMPLETED_BRANCH_CASH,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_COMPLETED_PLATFORM_SUMMARY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_FAILED_SENDER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRANSFER_CANCELLED_PARTIES,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.QR_CREATED_SENDER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.QR_CREATED_RECEIVER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.QR_COMPLETED_CASHIER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.QR_COMPLETED_SENDER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.QR_COMPLETED_RECEIVER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_TOPUP_REQUESTED_CASHIER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_TOPUP_COMPLETED_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_TOPUP_BRANCH_LARGE_CASH,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_WITHDRAW_REQUESTED_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_WITHDRAW_REQUESTED_CASHIER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_WITHDRAW_BRANCH_LIQUIDITY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.WALLET_WITHDRAW_COMPLETED_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.MERCHANT_PAYMENT_PAYER_RECEIPT,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.MERCHANT_PAYMENT_OWNER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.MERCHANT_PAYMENT_PLATFORM_DIGEST,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BILL_USER_STATUS,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BILL_MANUAL_CASHIER_QUEUE,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BILL_BRANCH_OVERDUE,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BATCH_SUBMITTED_CORPORATE,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BATCH_SUBMITTED_MOTHER_APPROVAL,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BATCH_COMPLETED_REPORT,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.BATCH_PLATFORM_HIGH_VOLUME,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.LOAN_DUE_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.LOAN_PAYMENT_FAILED_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.LOAN_DEFAULT_MOTHER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.LOAN_RISK_PLATFORM_DIGEST,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ESCROW_FUNDED_PARTY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ESCROW_RELEASED_PARTY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ESCROW_DISPUTED_PARTY,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ESCROW_CASHIER_WITNESS,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ESCROW_DISPUTE_MOTHER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRADING_ORDER_PLACED,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRADING_ORDER_FILLED,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRADING_ORDER_FAILED,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.TRADING_PLATFORM_DIGEST,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.AML_ALERT_AUDITOR,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.AML_ALERT_MOTHER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.AML_ALERT_PLATFORM_CRITICAL,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.LIQUIDITY_LOW_BRANCH,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ACCOUNT_FROZEN_USER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.ACCOUNT_FROZEN_INTERNAL,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.DISPUTE_OPENED_REPORTER,
            com.mycompany.transfersystem.notification.NotificationTemplateKeys.DISPUTE_RESOLVED_REPORTER,
            "internal.branch.ops",
            "trading.price.alert"
    };

    private void upsert(String templateKey, String locale, String body, NotificationMessageCategory category) {
        String loc = locale.toUpperCase(Locale.ROOT);
        if (notificationTemplateRepository.findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
                templateKey, loc, NotificationChannel.WHATSAPP).isPresent()) {
            return;
        }
        notificationTemplateRepository.save(NotificationTemplate.builder()
                .templateKey(templateKey)
                .locale(loc)
                .subject(null)
                .bodyTemplate(body)
                .channel(NotificationChannel.WHATSAPP)
                .messageCategory(category)
                .metaTemplateName(null)
                .build());
    }

    private static String englishBody(NotificationEventType t) {
        return switch (t) {
            case transfer_sender_created -> "ALMUKHTAR: You started transfer {{transactionId}} for {{amount}} {{currency}} to {{receiverName}}.";
            case transfer_receiver_pickup_ready -> "ALMUKHTAR: Pickup ready for transfer {{transactionId}}. Amount {{amount}} {{currency}}. Pickup code: {{pickupCode}}.";
            case transfer_sender_completed -> "ALMUKHTAR: Transfer {{transactionId}} completed. {{amount}} {{currency}} to {{receiverName}}. Pickup code for receiver: {{pickupCode}}.";
            case transfer_receiver_completed -> "ALMUKHTAR: Transfer {{transactionId}} is completed on your side. Amount {{amount}} {{currency}}.";
            case transfer_cancelled -> "ALMUKHTAR: Transfer {{transactionId}} was cancelled. Ref {{referenceId}}.";
            case transfer_failed -> "ALMUKHTAR: Transfer {{transactionId}} failed. {{detail}}";
            case qr_release_code_created -> "ALMUKHTAR: QR pickup is ready for transfer {{transactionId}}. Amount {{amount}} {{currency}}. Expires: {{expiresAt}}.";
            case qr_release_completed -> "ALMUKHTAR: QR release completed for transfer {{transactionId}}.";
            case wallet_topup_requested -> "ALMUKHTAR: Wallet top-up requested. Ref {{referenceId}}. {{detail}}";
            case wallet_topup_completed -> "ALMUKHTAR: Wallet top-up completed. Ref {{referenceId}}. {{detail}}";
            case wallet_withdrawal_ready -> "ALMUKHTAR: Withdrawal ready. Ref {{referenceId}}. {{detail}}";
            case merchant_payment_payer_receipt -> "ALMUKHTAR: Payment sent. Ref {{referenceId}}. {{detail}}";
            case merchant_payment_merchant_receipt -> "ALMUKHTAR: Payment received. Ref {{referenceId}}. {{detail}}";
            case bill_payment_processing -> "ALMUKHTAR: Bill payment processing. Ref {{referenceId}}. {{detail}}";
            case bill_payment_completed -> "ALMUKHTAR: Bill payment completed. Ref {{referenceId}}. {{detail}}";
            case batch_job_submitted -> "ALMUKHTAR: Batch job submitted. Ref {{referenceId}}. {{detail}}";
            case batch_job_completed -> "ALMUKHTAR: Batch job completed. Ref {{referenceId}}. {{detail}}";
            case loan_due_reminder -> "ALMUKHTAR: Loan reminder. Ref {{referenceId}}. {{detail}}";
            case loan_payment_success -> "ALMUKHTAR: Loan payment received. Ref {{referenceId}}. {{detail}}";
            case loan_payment_failed -> "ALMUKHTAR: Loan payment failed. Ref {{referenceId}}. {{detail}}";
            case escrow_funded -> "ALMUKHTAR: Escrow funded. Ref {{referenceId}}. {{detail}}";
            case escrow_released -> "ALMUKHTAR: Escrow released. Ref {{referenceId}}. {{detail}}";
            case escrow_disputed -> "ALMUKHTAR: Escrow dispute opened. Ref {{referenceId}}. {{detail}}";
            case trading_order_placed -> "ALMUKHTAR: Trading order placed. Ref {{referenceId}}. {{detail}}";
            case trading_order_filled -> "ALMUKHTAR: Trading order filled. Ref {{referenceId}}. {{detail}}";
            case trading_order_failed -> "ALMUKHTAR: Trading order failed. Ref {{referenceId}}. {{detail}}";
            case suspicious_activity_internal_alert -> "ALMUKHTAR INTERNAL: Suspicious activity alert. Ref {{referenceId}}. {{detail}}";
            case liquidity_low_branch_alert -> "ALMUKHTAR INTERNAL: Branch liquidity low. Ref {{referenceId}}. {{detail}}";
            case account_frozen_user_notice -> "ALMUKHTAR: Your account has a compliance hold. Ref {{referenceId}}. {{detail}}";
            case dispute_opened -> "ALMUKHTAR: A dispute was opened. Ref {{referenceId}}. {{detail}}";
            case dispute_resolved -> "ALMUKHTAR: Your dispute was resolved. Ref {{referenceId}}. {{detail}}";
            case system_maintenance -> "ALMUKHTAR: Scheduled maintenance notice. {{detail}}";
        };
    }

    private static String arabicBody(NotificationEventType t) {
        return switch (t) {
            case transfer_sender_created -> "المختار: بدأت حوالة {{transactionId}} بمبلغ {{amount}} {{currency}} إلى {{receiverName}}.";
            case transfer_receiver_pickup_ready -> "المختار: حوالة {{transactionId}} جاهزة للاستلام. المبلغ {{amount}} {{currency}}. رمز الاستلام: {{pickupCode}}.";
            case transfer_sender_completed -> "المختار: اكتملت حوالة {{transactionId}} بمبلغ {{amount}} {{currency}} إلى {{receiverName}}. رمز الاستلام للمستلم: {{pickupCode}}.";
            case transfer_receiver_completed -> "المختار: اكتملت حوالة {{transactionId}} من جهتك. المبلغ {{amount}} {{currency}}.";
            case transfer_cancelled -> "المختار: تم إلغاء الحوالة {{transactionId}}. مرجع {{referenceId}}.";
            case transfer_failed -> "المختار: فشلت الحوالة {{transactionId}}. {{detail}}";
            case qr_release_code_created -> "المختار: رمز QR جاهز للحوالة {{transactionId}}. المبلغ {{amount}} {{currency}}. الانتهاء: {{expiresAt}}.";
            case qr_release_completed -> "المختار: تم إكمال إطلاق QR للحوالة {{transactionId}}.";
            case wallet_topup_requested -> "المختار: طلب تعبئة محفظة. مرجع {{referenceId}}. {{detail}}";
            case wallet_topup_completed -> "المختار: اكتملت تعبئة المحفظة. مرجع {{referenceId}}. {{detail}}";
            case wallet_withdrawal_ready -> "المختار: السحب جاهز. مرجع {{referenceId}}. {{detail}}";
            case merchant_payment_payer_receipt -> "المختار: تم إرسال دفع. مرجع {{referenceId}}. {{detail}}";
            case merchant_payment_merchant_receipt -> "المختار: تم استلام دفع. مرجع {{referenceId}}. {{detail}}";
            case bill_payment_processing -> "المختار: جاري دفع فاتورة. مرجع {{referenceId}}. {{detail}}";
            case bill_payment_completed -> "المختار: اكتمل دفع الفاتورة. مرجع {{referenceId}}. {{detail}}";
            case batch_job_submitted -> "المختار: تم إرسال دفعة مجمعة. مرجع {{referenceId}}. {{detail}}";
            case batch_job_completed -> "المختار: اكتملت دفعة مجمعة. مرجع {{referenceId}}. {{detail}}";
            case loan_due_reminder -> "المختار: تذكير قرض. مرجع {{referenceId}}. {{detail}}";
            case loan_payment_success -> "المختار: تم استلام دفعة القرض. مرجع {{referenceId}}. {{detail}}";
            case loan_payment_failed -> "المختار: فشلت دفعة القرض. مرجع {{referenceId}}. {{detail}}";
            case escrow_funded -> "المختار: تم تمويل الضمان. مرجع {{referenceId}}. {{detail}}";
            case escrow_released -> "المختار: تم إطلاق الضمان. مرجع {{referenceId}}. {{detail}}";
            case escrow_disputed -> "المختار: نزاع على الضمان. مرجع {{referenceId}}. {{detail}}";
            case trading_order_placed -> "المختار: تم تقديم أمر تداول. مرجع {{referenceId}}. {{detail}}";
            case trading_order_filled -> "المختار: تم تنفيذ أمر تداول. مرجع {{referenceId}}. {{detail}}";
            case trading_order_failed -> "المختار: فشل أمر تداول. مرجع {{referenceId}}. {{detail}}";
            case suspicious_activity_internal_alert -> "المختار داخلي: تنبيه نشاط مشبوه. مرجع {{referenceId}}. {{detail}}";
            case liquidity_low_branch_alert -> "المختار داخلي: سيولة منخفضة في الفرع. مرجع {{referenceId}}. {{detail}}";
            case account_frozen_user_notice -> "المختار: قيود على حسابك لأسباب امتثال. مرجع {{referenceId}}. {{detail}}";
            case dispute_opened -> "المختار: تم فتح نزاع. مرجع {{referenceId}}. {{detail}}";
            case dispute_resolved -> "المختار: تم حل النزاع. مرجع {{referenceId}}. {{detail}}";
            case system_maintenance -> "المختار: إشعار صيانة مجدولة. {{detail}}";
        };
    }
}

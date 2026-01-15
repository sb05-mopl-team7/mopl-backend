package com.mopl.domain.auth.service;

import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final RedisManager redisManager;

    @Value("${spring.mail.username}")
    private String moplEmail;

    public void resetPassword(String email) {
        emailValid(email);
        String temporaryPassword = createTemporaryPassword(10);
        redisManager.save(RedisNameSpace.TEMP_PASSWORD, email, temporaryPassword);
        sendEmail(email, temporaryPassword);
    }


    /** 이메일 전송 */
    private void sendEmail(String email, String temporaryPassword) {
        var message = mailSender.createMimeMessage();

        try {
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(moplEmail, "모두의 플리");
            helper.setTo(email);
            helper.setSubject("임시 비밀번호가 발급되었습니다.");

            Context context = new Context();
            context.setVariable("temporaryPassword", temporaryPassword);
            String emailTemplate = templateEngine.process("reset-password", context);

            helper.setText(emailTemplate, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error occurred while sending email", e);
        }
    }

    /** 임시 비밀번호 생성 */
    private String createTemporaryPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        String PASSWORD_ALLOW_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(PASSWORD_ALLOW_CHARS.length());
            sb.append(PASSWORD_ALLOW_CHARS.charAt(randomIndex));
        }

        return sb.toString();
    }

    /** 이메일 검증 */
    private void emailValid(String email) {
        userRepository.findByEmail(email)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
    }


}

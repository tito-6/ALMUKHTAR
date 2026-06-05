package com.mycompany.transfersystem.service.feezone;

import com.mycompany.transfersystem.entity.enums.IslamicEvent;
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

@Service
@Slf4j
public class IslamicCalendarService {

    private final StringRedisTemplate redisTemplate;

    public IslamicCalendarService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isEventActive(IslamicEvent event, LocalDate gregorianDate) {
        String cacheKey = "islamic:" + event + ":" + gregorianDate;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return Boolean.parseBoolean(cached);
        } catch (Exception e) {
            log.debug("Redis unavailable for Islamic calendar cache");
        }

        boolean result = computeIsEventActive(event, gregorianDate);

        try {
            redisTemplate.opsForValue().set(cacheKey, String.valueOf(result), Duration.ofSeconds(86400));
        } catch (Exception e) {
            log.debug("Redis unavailable, skipping cache write");
        }
        return result;
    }

    private boolean computeIsEventActive(IslamicEvent event, LocalDate gregorianDate) {
        Date date = Date.from(gregorianDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        UmmalquraCalendar hijri = new UmmalquraCalendar();
        hijri.setTime(date);
        int month = hijri.get(Calendar.MONTH) + 1;
        int day = hijri.get(Calendar.DAY_OF_MONTH);

        return switch (event) {
            case RAMADAN -> month == 9;
            case EID_AL_FITR -> month == 10 && day >= 1 && day <= 3;
            case EID_AL_ADHA -> month == 12 && day >= 10 && day <= 13;
            case MUHARRAM -> month == 1;
        };
    }
}

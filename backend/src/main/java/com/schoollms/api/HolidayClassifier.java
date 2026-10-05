package com.schoollms.api;

import java.util.Locale;
import java.util.regex.Pattern;

final class HolidayClassifier {
  private static final Pattern HOLIDAY_TERMS =
      Pattern.compile(
          "\\b(holidays?|festival|school\\s+closed|no\\s+school|vacation|winter\\s+break|summer\\s+break|term\\s+break|diwali|deepavali|holi|ganesh\\s+chaturthi|ganpati|dussehra|vijayadashami|navratri|durga\\s+puja|eid(?:-e-fitr|-ul-fitr|-al-fitr|-ul-adha|-al-adha|-e-milad)?|bakrid|muharram|christmas|good\\s+friday|gur[u]?\\s+nanak|janmashtami|krishna\\s+janmashtami|ram\\s+navami|maha\\s+shivaratri|shivaratri|gudi\\s+padwa|makar\\s+sankranti|pongal|buddha\\s+purnima|mahavir\\s+jayanti|republic\\s+day|independence\\s+day|maharashtra\\s+day|ambedkar\\s+jayanti|gandhi\\s+jayanti|anant\\s+chaturdashi)\\b",
          Pattern.CASE_INSENSITIVE);

  private HolidayClassifier() {}

  static boolean isHoliday(String title, String description) {
    String text = ((title == null ? "" : title) + " " + (description == null ? "" : description))
        .toLowerCase(Locale.ROOT);
    return HOLIDAY_TERMS.matcher(text).find();
  }
}

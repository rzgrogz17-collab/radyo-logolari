package ogzapp.wordgame.ui.calendar;

public interface DateUtil {


    Date newDate();
    Date newDate(long millis);
    Date newDate(int year, int month);
    Date newDate(int year, int month, int date);


}

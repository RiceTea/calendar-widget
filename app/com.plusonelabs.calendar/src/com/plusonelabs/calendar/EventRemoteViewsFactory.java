package com.plusonelabs.calendar;

import static com.plusonelabs.calendar.prefs.ICalendarPreferences.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.plusonelabs.calendar.calendar.CalendarEventVisualizer;
import com.plusonelabs.calendar.model.DayHeader;
import com.plusonelabs.calendar.model.EventEntry;

public class EventRemoteViewsFactory implements RemoteViewsFactory {

	private static final String EMPTY_STRING = "";
	private static final String DAY_STRING_FORMAT = "EEEE, ";
	private static final String DAY_DATE_FORMAT = "dd. MMMM";

	private static final String COMMA_SPACE = ", ";

	static SimpleDateFormat dayDateFormatter;
	static SimpleDateFormat dayStringFormatter;

	private final Context context;
	private SharedPreferences prefs;
	private ArrayList<EventEntry> eventEntries;

	private ArrayList<IEventVisualizer<?>> eventProviders;

	static {
		initiDateFormatter();
	}

	public EventRemoteViewsFactory(Context context) {
		this.context = context;
		eventProviders = new ArrayList<IEventVisualizer<?>>();
		eventProviders.add(new CalendarEventVisualizer(context));
		eventEntries = new ArrayList<EventEntry>();
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}

	public void onCreate() {
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
		rv.setPendingIntentTemplate(R.id.event_list,
				CalendarIntentUtil.createOpenCalendarEventPendingIntent(context));
	}

	public void onDestroy() {
		eventEntries.clear();
	}

	public int getCount() {
		return eventEntries.size();
	}

	public RemoteViews getViewAt(int position) {
		EventEntry entry = eventEntries.get(position);
		if (entry instanceof DayHeader) {
			return updateDayHeader((DayHeader) entry);
		}
		for (int i = 0; i < eventProviders.size(); i++) {
			IEventVisualizer<?> eventProvider = eventProviders.get(i);
			if (entry.getClass().isAssignableFrom(eventProvider.getSupportedEventEntryType())) {
				return eventProvider.getRemoteView(entry);
			}
		}
		return null;
	}

	public static void initiDateFormatter() {
		dayDateFormatter = new SimpleDateFormat(DAY_DATE_FORMAT);
		dayStringFormatter = new SimpleDateFormat(DAY_STRING_FORMAT);
	}

	public RemoteViews updateDayHeader(DayHeader dayHeader) {
		RemoteViews rv = new RemoteViews(context.getPackageName(), getDayHeaderLayout());
		rv.setTextViewText(R.id.day_header_title, createDayEntryString(dayHeader));
		Intent intent = CalendarIntentUtil.createOpenCalendarAtDayIntent(context,
				dayHeader.getStartDate());
		rv.setOnClickFillInIntent(R.id.day_header, intent);
		return rv;
	}

	private int getDayHeaderLayout() {
		String textSize = prefs.getString(PREF_TEXT_SIZE, PREF_TEXT_SIZE_MEDIUM);
		if (textSize.equals(PREF_TEXT_SIZE_SMALL)) {
			return R.layout.day_header_small;
		} else if (textSize.equals(PREF_TEXT_SIZE_LARGE)) {
			return R.layout.day_header_large;
		}
		return R.layout.day_header_medium;
	}

	public String createDayEntryString(DayHeader dayEntry) {
		long date = dayEntry.getStartDate();
		String prefix = EMPTY_STRING;
		if (dayEntry.isToday()) {
			prefix = context.getString(R.string.today) + COMMA_SPACE;
		} else if (dayEntry.isTomorrow()) {
			prefix = context.getString(R.string.tomorrow) + COMMA_SPACE;
		} else {
			prefix = dayStringFormatter.format(new Date(date)).toUpperCase();
		}
		return prefix + dayDateFormatter.format(new Date(date)).toUpperCase();
	}

	public void onDataSetChanged() {
		eventEntries.clear();
		ArrayList<EventEntry> events = new ArrayList<EventEntry>();
		for (int i = 0; i < eventProviders.size(); i++) {
			events.addAll(eventProviders.get(i).getEventEntries());
		}
		updateEntryList(events);
	}

	public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
		if (recycle == null) {
			recycle = new Time();
		}
		recycle.timezone = tz;
		recycle.set(localTime);
		recycle.timezone = Time.TIMEZONE_UTC;
		return recycle.normalize(true);
	}

	public void updateEntryList(ArrayList<EventEntry> eventList) {
		if (!eventList.isEmpty()) {
			EventEntry entry = eventList.get(0);
			DayHeader curDayBucket = new DayHeader(DateUtil.getStartDateInUTC(entry));
			eventEntries.add(curDayBucket);
			for (EventEntry event : eventList) {
				long startDateInUTC = DateUtil.getStartDateInUTC(event);
				if (!DateUtil.isSameDay(startDateInUTC, curDayBucket.getStartDate())) {
					curDayBucket = new DayHeader(startDateInUTC);
					eventEntries.add(curDayBucket);
				}
				eventEntries.add(event);
			}
		}
	}

	public RemoteViews getLoadingView() {
		return null;
	}

	public int getViewTypeCount() {
		int result = 0;
		for (int i = 0; i < eventProviders.size(); i++) {
			IEventVisualizer<?> eventProvider = eventProviders.get(i);
			result += eventProvider.getViewTypeCount();
		}
		return result;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

}

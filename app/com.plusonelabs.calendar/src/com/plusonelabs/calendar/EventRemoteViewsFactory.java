package com.plusonelabs.calendar;

import static com.plusonelabs.calendar.CalendarIntentUtil.*;
import static com.plusonelabs.calendar.RemoteViewsUtil.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.plusonelabs.calendar.calendar.CalendarEventVisualizer;
import com.plusonelabs.calendar.model.DayHeader;
import com.plusonelabs.calendar.model.Event;

public class EventRemoteViewsFactory implements RemoteViewsFactory {

	private static final String EMPTY_STRING = "";
	private static final String COMMA_SPACE = ", ";

	private final Context context;
	private final ArrayList<Event> eventEntries;

	private final ArrayList<IEventVisualizer<?>> eventProviders;

	public EventRemoteViewsFactory(Context context) {
		this.context = context;
		eventProviders = new ArrayList<IEventVisualizer<?>>();
		eventProviders.add(new CalendarEventVisualizer(this.context));
		eventEntries = new ArrayList<Event>();
	}

	public void onCreate() {
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
		rv.setPendingIntentTemplate(R.id.event_list, createOpenCalendarEventPendingIntent(context));
	}

	public void onDestroy() {
		eventEntries.clear();
	}

	public int getCount() {
		return eventEntries.size();
	}

	public RemoteViews getViewAt(int position) {
		if (position < eventEntries.size()) {
			Event entry = eventEntries.get(position);
			if (entry instanceof DayHeader) {
				return updateDayHeader((DayHeader) entry);
			}
			for (int i = 0; i < eventProviders.size(); i++) {
				IEventVisualizer<?> eventProvider = eventProviders.get(i);
				if (entry.getClass().isAssignableFrom(eventProvider.getSupportedEventEntryType())) {
					return eventProvider.getRemoteView(entry);
				}
			}
		}
		return null;
	}

	public RemoteViews updateDayHeader(DayHeader dayHeader) {
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.day_header);
		rv.setTextViewText(R.id.day_header_title, createDayEntryString(dayHeader));
		setTextSize(context, rv, R.id.day_header_title, R.dimen.day_header_title);
		setTextColorRes(context, rv, R.id.day_header_title, R.attr.dayHeaderTitle);
		setBackgroundColorRes(context, rv, R.id.day_header_separator, R.attr.dayHeaderSeparator);
		setPadding(context, rv, R.id.day_header_title, 0, R.dimen.day_header_padding_top,
				R.dimen.day_header_padding_right, R.dimen.day_header_padding_bottom);
		Intent intent = createOpenCalendarAtDayIntent(context, dayHeader.getStartDate());
		rv.setOnClickFillInIntent(R.id.day_header, intent);
		return rv;
	}

	public String createDayEntryString(DayHeader dayEntry) {
		Date date = dayEntry.getStartDate().toDate();
		String prefix = EMPTY_STRING;
		if (dayEntry.isToday()) {
			prefix = context.getString(R.string.today) + COMMA_SPACE;
			return prefix
					+ DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_DATE)
							.toUpperCase(Locale.getDefault());
		} else if (dayEntry.isTomorrow()) {
			prefix = context.getString(R.string.tomorrow) + COMMA_SPACE;
			return prefix
					+ DateUtils.formatDateTime(context, date.getTime(), DateUtils.FORMAT_SHOW_DATE)
							.toUpperCase(Locale.getDefault());
		}
		return DateUtils.formatDateTime(context, date.getTime(),
				DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY).toUpperCase(
				Locale.getDefault());
	}

	public void onDataSetChanged() {
		eventEntries.clear();
		ArrayList<Event> events = new ArrayList<Event>();
		for (int i = 0; i < eventProviders.size(); i++) {
			events.addAll(eventProviders.get(i).getEventEntries());
		}
		updateEntryList(events);
	}

	public void updateEntryList(ArrayList<Event> eventList) {
		if (!eventList.isEmpty()) {
			Event firstEvent = eventList.get(0);
			DayHeader curDayBucket = new DayHeader(firstEvent.getStartDate());
			eventEntries.add(curDayBucket);
			for (Event event : eventList) {
				DateTime startDate = event.getStartDate();
				if (!startDate.toDateMidnight().isEqual(
						curDayBucket.getStartDate().toDateMidnight())) {
					curDayBucket = new DayHeader(startDate);
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

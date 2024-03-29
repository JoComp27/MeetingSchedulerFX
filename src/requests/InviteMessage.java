package requests;

import Tools.CalendarUtil;

import java.util.Calendar;

public class InviteMessage extends Message {

    private Integer meetingNumber;
    private Calendar calendar;
    private String topic;
    private String requester;

    public InviteMessage() {
        super(RequestType.Invite);
        this.meetingNumber = null;
        this.calendar = null;
        this.topic = null;
        this.requester = null;
    }

    public InviteMessage(Integer meetingNumber, Calendar calendar, String topic, String requester) {
        super(RequestType.Invite);
        this.meetingNumber = meetingNumber;
        this.calendar = calendar;
        this.topic = topic;
        this.requester = requester;
    }

    public Integer getMeetingNumber() {
        return meetingNumber;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public String getTopic() {
        return topic;
    }

    public String getRequester() {
        return requester;
    }

    public void setMeetingNumber(Integer meetingNumber) {
        this.meetingNumber = meetingNumber;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    @Override
    public String serialize() {

        String msg = "";
        msg += requestType.ordinal() + "$";
        msg += meetingNumber + "$";
        msg += CalendarUtil.calendarToString(calendar) + "$";
        msg += topic.trim() + "$";
        msg += requester;

        return msg;
    }

    @Override
    public void deserialize(String message) {

        String[] arrMsg = message.split("\\$");
        Calendar c = CalendarUtil.stringToCalendar(arrMsg[2]);

        this.meetingNumber = Integer.parseInt(arrMsg[1]);
        this.calendar = c;
        this.topic = arrMsg[3];
        this.requester = arrMsg[4];

    }
}

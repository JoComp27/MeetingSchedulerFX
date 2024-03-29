package client;

import Tools.CalendarUtil;
import requests.ConfirmMessage;
import requests.InviteMessage;
import requests.RequestMessage;
import requests.ScheduledMessage;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ClientMeeting {

    //Common Arguments
    private Calendar calendar; // Date and time of the meeting
    private boolean userType; // False -> Invitee ; True -> Requester
    private boolean state; // False -> Standby ; True -> Confirmed
    private boolean currentAnswer; // User's current reply to request
    private int roomNumber; // Room number assigned to meeting

    //Requester Arguments
    private int requestNumber;
    //Key=port, bool=approved
    private HashMap<String, Boolean> acceptedMap;

    //Invitee Arguments
    private int meetingNumber;

    public ClientMeeting() { //Used to restore saves
    }

    public ClientMeeting(InviteMessage inviteMessage) { //Invitee Meeting
        this.meetingNumber = inviteMessage.getMeetingNumber();
        this.requestNumber = -1;
        this.calendar = inviteMessage.getCalendar();
        this.state = false;
        this.userType = false;
        this.acceptedMap = new HashMap<>();

    }

    public ClientMeeting(RequestMessage requestMessage) { //Requester Meeting
        this.requestNumber = requestMessage.getRequestNumber();
        this.meetingNumber = -1;
        this.calendar = requestMessage.getCalendar();
        this.state = false;
        this.userType = true;
        this.currentAnswer = true;
        this.acceptedMap = new HashMap<>();
    }

    public void receiveConfirmMessage(ConfirmMessage confirmMessage) {

        this.state = true;
        this.roomNumber = confirmMessage.getRoomNumber();
        this.meetingNumber = confirmMessage.getMeetingNumber();

    }

    public void receiveScheduledMessage(ScheduledMessage scheduledMessage) {

        this.state = true;
        this.meetingNumber = scheduledMessage.getMeetingNumber();
        this.roomNumber = scheduledMessage.getRoomNumber();
        this.acceptedMap = new HashMap<>();

        for (String participant : scheduledMessage.getListOfConfirmedParticipants()) {
            acceptedMap.put(participant, true);
        }

    }

    public boolean getUserType() {
        return userType;
    }

    public boolean getState() {
        return state;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public int getRequestNumber() {
        return requestNumber;
    }

    public HashMap<String, Boolean> getAcceptedMap() {
        return acceptedMap;
    }

    public int getMeetingNumber() {
        return meetingNumber;
    }

    public boolean isCurrentAnswer() {
        return currentAnswer;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setCurrentAnswer(boolean currentAnswer) {
        this.currentAnswer = currentAnswer;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setMeetingNumber(int meetingNumber) {
        this.meetingNumber = meetingNumber;
    }

    public String serialize() {
        String result = "";

        result += CalendarUtil.calendarToString(calendar) + ",";
        result += userType + ",";
        result += state + ",";
        result += currentAnswer + ",";
        result += roomNumber + ",";
        result += requestNumber + ",";
        result += meetingNumber + ",";

        for (Map.Entry<String, Boolean> entry : acceptedMap.entrySet()) {
            result += entry.getKey() + "!" + entry.getValue() + "@";
        }

        return result;
    }

    public void deserialize(String message) {

        String[] subMessages = message.split(",");

        this.calendar = CalendarUtil.stringToCalendar(subMessages[0]);
        this.userType = Boolean.parseBoolean(subMessages[1]);
        this.state = Boolean.parseBoolean(subMessages[2]);
        this.currentAnswer = Boolean.parseBoolean(subMessages[3]);
        this.roomNumber = Integer.parseInt(subMessages[4]);
        this.requestNumber = Integer.parseInt(subMessages[5]);
        this.meetingNumber = Integer.parseInt(subMessages[6]);

        if (subMessages.length > 7 && !subMessages[7].isEmpty()) {
            String[] acceptedMap = subMessages[7].split("@");

            for (String accMsg : acceptedMap) {

                if (accMsg.isEmpty()) {
                    continue;
                }

                String[] entry = accMsg.split("!");
                this.acceptedMap.put(entry[0], Boolean.parseBoolean(entry[1]));
            }

        } else {
            this.acceptedMap = new HashMap<>();
        }

    }
}

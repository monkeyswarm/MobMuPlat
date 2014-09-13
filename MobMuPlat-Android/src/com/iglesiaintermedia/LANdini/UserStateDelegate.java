package com.iglesiaintermedia.LANdini;

import java.util.List;

public interface UserStateDelegate {
	
	public void userStateChanged(List<LANdiniUser> userList);
	public void syncServerChanged(String newServerName);


}

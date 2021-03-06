/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.reporting.event_store;

import java.util.Set;
import javax.persistence.*;

import org.hibernate.annotations.Entity;

@Entity
@javax.persistence.Entity
@SqlResultSetMapping(name = "createEventMap", entities = @EntityResult(entityClass = ReportingInstanceCreateEvent.class))
@NamedNativeQuery(name = "scanInstanceCreateEvents", query = "select * from reporting_instance_create_events order by timestamp_ms", resultSetMapping = "createEventMap")
@PersistenceContext(name = "eucalyptus_reporting")
@Table(name = "reporting_instance_create_events")
public class ReportingInstanceCreateEvent extends ReportingEventSupport {
    private static final long serialVersionUID = 1L;

    @Column(name = "uuid", nullable = false, unique = true)
    private String uuid;
    @Column(name = "instance_id", nullable = false)
    private String instanceId;
    @Column(name = "instance_type", nullable = false)
    private String instanceType;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "user_name", nullable = false)
    private String userName;
    @Column(name = "account_name", nullable = false)
    private String accoutName;
    @Column(name = "account_id", nullable = false)
    private String accountId;
    @Column(name = "availability_zone", nullable = false)
    private String availabilityZone;

    /**
     * <p>
     * Do not instantiate this class directly; use the
     * ReportingInstanceEventStore class.
     */
    protected ReportingInstanceCreateEvent() {
    }

    /**
     * <p>
     * Do not instantiate this class directly; use the
     * ReportingInstanceEventStore class.
     */
    ReportingInstanceCreateEvent(String uuid, Long timestampMs,
	    String instanceId, String instanceType, String userId,
	    String userName, String accountName, String accountId,
	    String availabilityZone) {
	this.uuid = uuid;
	this.timestampMs = timestampMs;
	this.instanceId = instanceId;
	this.instanceType = instanceType;
	this.userId = userId;
	this.userName = userName;
	this.accoutName = accountName;
	this.accountId = accountId;
	this.availabilityZone = availabilityZone;
    }

    @Override
    public EventDependency asDependency() {
	return asDependency("uuid", uuid);
    }

    @Override
    public Set<EventDependency> getDependencies() {
	return withDependencies().user(userId).set();
    }

    @Override
    public int hashCode() {
	return (uuid == null) ? 0 : uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (getClass() != obj.getClass())
	    return false;
	ReportingInstanceCreateEvent other = (ReportingInstanceCreateEvent) obj;
	if (uuid == null) {
	    if (other.uuid != null)
		return false;
	} else if (!uuid.equals(other.uuid))
	    return false;
	return true;
    }

    public String getUuid() {
	return uuid;
    }

    public String getInstanceId() {
	return instanceId;
    }

    public String getInstanceType() {
	return instanceType;
    }

    public String getUserId() {
	return userId;
    }

    public String getUserName() {
	return userName;
    }

    public String getAccoutName() {
	return accoutName;
    }

    public String getAccountId() {
	return accountId;
    }

    public String getAvailabilityZone() {
	return availabilityZone;
    }

    @Override
    public String toString() {
	return "ReportingInstanceCreateEvent [uuid=" + uuid + ", instanceId="
		+ instanceId + ", instanceType=" + instanceType + ", userId="
		+ userId + ", userName=" + userName + ", accoutName="
		+ accoutName + ", accountId=" + accountId
		+ ", availabilityZone=" + availabilityZone + "]";
    }


}

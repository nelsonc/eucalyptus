package com.eucalyptus.reporting.modules.storage

import org.junit.Test
import com.eucalyptus.auth.principal.Principals

import static org.junit.Assert.*
import com.eucalyptus.reporting.domain.ReportingAccountCrud
import com.eucalyptus.reporting.domain.ReportingUserCrud
import com.eucalyptus.auth.principal.User
import com.google.common.base.Charsets
import com.eucalyptus.reporting.event.VolumeEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeEventStore
import com.eucalyptus.reporting.event_store.ReportingVolumeCreateEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeDeleteEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeAttachEvent
import com.eucalyptus.reporting.event_store.ReportingVolumeDetachEvent

/**
 * 
 */
class VolumeUsageEventListenerTest {

  @Test
  void testInstantiable() {
    new VolumeUsageEventListener()
  }

  @Test
  void testCreateEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( VolumeEvent.with(
        VolumeEvent.forVolumeCreate(),
        uuid("vol-00000001"),
        "vol-00000001",
        1234L,
        Principals.systemFullName() ,
        "PARTI00"
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeCreateEvent", persisted instanceof ReportingVolumeCreateEvent )
    ReportingVolumeCreateEvent event = persisted
    assertEquals( "Persisted event uuid", uuid("vol-00000001"), event.getUuid() )
    assertEquals( "Persisted event name", "vol-00000001", event.getVolumeId() )
    assertEquals( "Persisted event zone", "PARTI00", event.getAvailabilityZone() )
    assertEquals( "Persisted event size", 1234L, event.getSizeGB() )
    assertEquals( "Persisted event user id", Principals.systemFullName().getUserId(), event.getUserId() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDeleteEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( VolumeEvent.with(
        VolumeEvent.forVolumeDelete(),
        uuid("vol-00000001"),
        "vol-00000001",
        1234L,
        Principals.systemFullName() ,
        "PARTI00"
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeDeleteEvent", persisted instanceof ReportingVolumeDeleteEvent )
    ReportingVolumeDeleteEvent event = persisted
    assertEquals( "Persisted event uuid", uuid("vol-00000001"), event.getUuid() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testAttachEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( VolumeEvent.with(
        VolumeEvent.forVolumeAttach( uuid("i-00000002"), "i-00000002" ),
        uuid("vol-00000001"),
        "vol-00000001",
        12345L,
        Principals.systemFullName() ,
        "PARTI00"
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeAttachEvent", persisted instanceof ReportingVolumeAttachEvent )
    ReportingVolumeAttachEvent event = persisted
    assertEquals( "Persisted event volume uuid", uuid("vol-00000001"), event.getVolumeUuid() )
    assertEquals( "Persisted event instance uuid", uuid("i-00000002"), event.getInstanceUuid() )
    assertEquals( "Persisted event size", 12345L, event.getSizeGB() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  @Test
  void testDetachEvent() {
    long timestamp = System.currentTimeMillis() - 100000

    Object persisted = testEvent( VolumeEvent.with(
        VolumeEvent.forVolumeDetach( uuid("i-00000002"), "i-00000002" ),
        uuid("vol-00000001"),
        "vol-00000001",
        12345L,
        Principals.systemFullName() ,
        "PARTI00"
    ), timestamp )

    assertTrue( "Persisted event is ReportingVolumeAttachEvent", persisted instanceof ReportingVolumeDetachEvent )
    ReportingVolumeDetachEvent event = persisted
    assertEquals( "Persisted event volume uuid", uuid("vol-00000001"), event.getVolumeUuid() )
    assertEquals( "Persisted event instance uuid", uuid("i-00000002"), event.getInstanceUuid() )
    assertEquals( "Persisted event size", 12345L, event.getSizeGB() )
    assertEquals( "Persisted event timestamp", timestamp, event.getTimestampMs() )
  }

  private Object testEvent( VolumeEvent event, long timestamp ) {
    String updatedAccountId = null
    String updatedAccountName = null
    String updatedUserId = null
    String updatedUserName = null
    Object persisted = null
    ReportingAccountCrud accountCrud = new ReportingAccountCrud( ) {
      @Override void createOrUpdateAccount( String id, String name ) {
        updatedAccountId = id
        updatedAccountName = name
      }
    }
    ReportingUserCrud userCrud = new ReportingUserCrud( ) {
      @Override void createOrUpdateUser( String id, String accountId, String name ) {
        updatedUserId = id
        updatedUserName = name
      }
    }
    ReportingVolumeEventStore eventStore = new ReportingVolumeEventStore( ) {
      @Override protected void persist( final Object o ) {
        persisted = o
      }
    }
    VolumeUsageEventListener listener = new VolumeUsageEventListener( ) {
      @Override protected ReportingAccountCrud getReportingAccountCrud() { return accountCrud }
      @Override protected ReportingUserCrud getReportingUserCrud() { return userCrud }
      @Override protected ReportingVolumeEventStore getReportingVolumeEventStore() { eventStore }
      @Override protected long getCurrentTimeMillis() { timestamp }
      @Override protected User lookupUser( final String userId ) {
        assertEquals( "Looked up user", "eucalyptus", userId )
        Principals.systemUser()
      }
    }

    listener.fireEvent( event )

    assertNotNull( "Persisted event", persisted )
    assertEquals( "Account Id", "eucalyptus", updatedAccountId  )
    assertEquals( "Account Name", "000000000000", updatedAccountName )
    assertEquals( "User Id", "eucalyptus", updatedUserId )
    assertEquals( "User Name", "eucalyptus", updatedUserName )

    persisted
  }

  private String uuid( String seed ) {
    return UUID.nameUUIDFromBytes( seed.getBytes(Charsets.UTF_8) ).toString()
  }}

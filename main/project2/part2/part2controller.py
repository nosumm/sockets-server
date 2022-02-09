# Part 2 of UWCSE's Project 3
#
# based on Lab 4 from UCSC's Networking Class
# which is based on of_tutorial by James McCauley

from pox.core import core
import pox.openflow.libopenflow_01 as of
import pox.lib.packet as pkt

log = core.getLogger()

class Firewall (object):
  """
  A Firewall object is created for each switch that connects.
  A Connection object for that switch is passed to the __init__ function.
  """
  def __init__ (self, connection):
    # Keep track of the connection to the switch so that we can
    # send it messages!
    self.connection = connection

    # This binds our PacketIn event listener
    connection.addListeners(self)

    #add switch rules here
    icmp_msg = of.ofp_flow_mod()
    icmp_msg.match.dl_type = pkt.ethernet.IP_TYPE
    icmp_msg.match.nw_proto = pkt.ipv4.ICMP_PROTOCOL
    icmp_msg.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    connection.send(icmp_msg)

    arp_msg = of.ofp_flow_mod()
    arp_msg.match.dl_type = pkt.ethernet.ARP_TYPE
    arp_msg.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
    connection.send(arp_msg)

    drop_msg = of.ofp_flow_mod()
    drop_msg.match.nw_proto = None
    drop_msg.match.dl_type = None
    connection.send(drop_msg)

  def _handle_PacketIn (self, event):
    """
    Packets not handled by the router rules will be
    forwarded to this method to be handled by the controller
    """

    packet = event.parsed # This is the parsed packet data.
    if not packet.parsed:
      log.warning("Ignoring incomplete packet")
      return

    packet_in = event.ofp # The actual ofp_packet_in message.
    print ("Unhandled packet :" + str(packet.dump()))

def launch ():
  """
  Starts the component
  """
  def start_switch (event):
    log.debug("Controlling %s" % (event.connection,))
    Firewall(event.connection)
  core.openflow.addListenerByName("ConnectionUp", start_switch)

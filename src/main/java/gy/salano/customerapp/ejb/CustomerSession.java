/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gy.salano.customerapp.ejb;

import gy.salano.customerapp.entity.Customer;
import gy.salano.customerapp.entity.DiscountCode;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Salano
 */
@Stateless
@LocalBean
public class CustomerSession {

    @Resource(mappedName = "jms/NotificationQueue")
    private Queue notificationQueue;
    @Resource(mappedName = "java:comp/DefaultJMSConnectionFactory")
    private ConnectionFactory java_compDefaultJMSConnectionFactory;

    @PersistenceContext
    private EntityManager em;

    public List<Customer> retrieve() {
        //Query query = em.createNamedQuery("Customer.findAll",Customer.class);
        Query query = em.createNamedQuery("Customer.findAll", Customer.class);
        return query.getResultList();
    }
 /**
     * Included here for convenience rather than creating a new Session Bean
     * @return List<DiscountCode>
     */
    public List<DiscountCode> getDiscountCodes()
    {
        Query query = em.createNamedQuery("DiscountCode.findAll");
        return query.getResultList();
    }
    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public Customer Update(Customer customer) {
        Customer updated = em.merge(customer);
        try {
            sendJMSMessageToNotificationQueue(updated);
        } catch (JMSException ex) {
            Logger.getLogger(CustomerSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Customer updated in CustomerSession!");
        return updated;
    }
    public Customer createCustomer(Customer customer){
        em.persist(customer);
        try {
            sendJMSMessageToNotificationQueue(customer);
        } catch (JMSException ex) {
            Logger.getLogger(CustomerSession.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Customer created in CustomerSession!");
        return customer;
    }

    private Message createJMSMessageForjmsNotificationQueue(Session session, Object messageData) throws JMSException {
        //Modified to use ObjectMessage instead    
        ObjectMessage tm = session.createObjectMessage();    
	tm.setObject((Serializable) messageData);   
	return tm;
    }

    private void sendJMSMessageToNotificationQueue(Object messageData) throws JMSException {
        Connection connection = null;
        Session session = null;
        try {
            connection = java_compDefaultJMSConnectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer messageProducer = session.createProducer(notificationQueue);
            messageProducer.send(createJMSMessageForjmsNotificationQueue(session, messageData));
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot close session", e);
                }
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}

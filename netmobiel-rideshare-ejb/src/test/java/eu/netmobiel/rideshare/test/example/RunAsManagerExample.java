package eu.netmobiel.rideshare.test.example;

import java.util.concurrent.Callable;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;

/**
 * @see https://samaxes.com/2014/11/test-javaee-security-with-arquillian/
 * 
 * @author Jaap Reitsma
 *
 */
//@Stateless
@RunAs("Manager")
@PermitAll
public class RunAsManagerExample {
    public <V> V call(Callable<V> callable) throws Exception {
        return callable.call();
    }
}

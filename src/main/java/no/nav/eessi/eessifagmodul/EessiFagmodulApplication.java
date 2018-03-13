package no.nav.eessi.eessifagmodul;

import io.prometheus.client.hotspot.DefaultExports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EessiFagmodulApplication {

    private static Logger log = LoggerFactory.getLogger(EessiFagmodulApplication.class);

	public static void main(String[] args) {
	    DefaultExports.initialize();
        SpringApplication.run(EessiFagmodulApplication.class, args);
    }
}

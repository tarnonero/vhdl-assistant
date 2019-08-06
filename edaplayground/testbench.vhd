library ieee;
use ieee.std_logic_1164.all;

entity half_adder_tb is
end half_adder_tb;

architecture behav of half_adder_tb is

	component half_adder
    	port(x, y, enable: in bit;
        	 carry, result: out bit);
    end component;
    
    constant period: time := 20 ns;
    signal x: bit := '0';
    signal y: bit := '0';
    signal enable: bit := '0';
    signal carry ,result: bit;
  	
    begin    
    ha: half_adder port map(
    	x => x,
        y => y,
        enable => enable,
        carry => carry,
        result => result
    );
    process
    	begin          
             wait for period;
             
        	 x  <= '0';
    		 y  <= '1';
    		 wait for period;

             x  <= '1';
             y  <= '0';
             wait for period;

             x  <= '1';
             y  <= '1';
             wait for period;
             
             enable <= '1';             
             x  <= '0';
    		 y  <= '1';
             wait for period;
             
             x  <= '1';
    		 y  <= '0';
             wait for period;
             
             x  <= '1';
    		 y  <= '1';
             wait;
             
    end process;
end behav;																										
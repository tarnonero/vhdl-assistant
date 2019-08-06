entity xor2 is
	port(in0, in1: in bit;
    	 out0: out bit);
end xor2;

architecture behav of xor2 is
	begin 
    	out0 <= in0 xor in1;
end behav;
--
-- PostgreSQL database dump
--

-- Dumped from database version 13.20 (Debian 13.20-1.pgdg120+1)
-- Dumped by pg_dump version 13.20 (Debian 13.20-1.pgdg120+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: application_user; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.application_user (
    id integer NOT NULL,
    version integer,
    user_name character varying(20) NOT NULL,
    passwd character varying(20) NOT NULL,
    role character varying(255) NOT NULL
);


ALTER TABLE public.application_user OWNER TO creator;

--
-- Name: category; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.category (
    id integer NOT NULL,
    version integer,
    category_name character varying(40) NOT NULL
);


ALTER TABLE public.category OWNER TO creator;

--
-- Name: draft; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.draft (
    id integer NOT NULL,
    version integer,
    availability character varying(255),
    price numeric(19,2),
    product_id integer,
    product_name character varying(255),
    stock_count integer,
    user_id integer
);


ALTER TABLE public.draft OWNER TO creator;

--
-- Name: draft_category; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.draft_category (
    draft_id integer NOT NULL,
    category_id integer NOT NULL
);


ALTER TABLE public.draft_category OWNER TO creator;

--
-- Name: idgenerator; Type: SEQUENCE; Schema: public; Owner: creator
--

CREATE SEQUENCE public.idgenerator
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.idgenerator OWNER TO creator;

--
-- Name: message; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.message (
    id integer NOT NULL,
    version integer,
    date_stamp timestamp without time zone NOT NULL,
    message character varying(255) NOT NULL
);


ALTER TABLE public.message OWNER TO creator;

--
-- Name: product; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.product (
    id integer NOT NULL,
    version integer,
    availability character varying(255) NOT NULL,
    price numeric(19,2),
    product_name character varying(100) NOT NULL,
    stock_count integer NOT NULL,
    CONSTRAINT product_price_check CHECK ((price >= (0)::numeric)),
    CONSTRAINT product_stock_count_check CHECK ((stock_count >= 0))
);


ALTER TABLE public.product OWNER TO creator;

--
-- Name: product_category; Type: TABLE; Schema: public; Owner: creator
--

CREATE TABLE public.product_category (
    product_id integer NOT NULL,
    category_id integer NOT NULL
);


ALTER TABLE public.product_category OWNER TO creator;

--
-- Data for Name: application_user; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.application_user (id, version, user_name, passwd, role) FROM stdin;
152	0	User0	user0	USER
153	0	User1	user1	USER
154	0	User2	user2	USER
155	0	User3	user3	USER
156	0	User4	user4	USER
157	0	User5	user5	USER
158	0	User6	user6	USER
159	0	User7	user7	USER
160	0	User8	user8	USER
161	0	User9	user9	USER
162	0	Admin	admin	ADMIN
163	0	Super	super	ADMIN
\.


--
-- Data for Name: category; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.category (id, version, category_name) FROM stdin;
1	0	Children's books
2	0	Best sellers
3	0	Romance
4	0	Mystery
5	0	Thriller
6	0	Sci-fi
7	0	Non-fiction
8	0	Cookbooks
\.


--
-- Data for Name: draft; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.draft (id, version, availability, price, product_id, product_name, stock_count, user_id) FROM stdin;
\.


--
-- Data for Name: draft_category; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.draft_category (draft_id, category_id) FROM stdin;
\.


--
-- Data for Name: message; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.message (id, version, date_stamp, message) FROM stdin;
202	0	2025-02-24 20:15:55.873209	System update complete
\.


--
-- Data for Name: product; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.product (id, version, availability, price, product_name, stock_count) FROM stdin;
52	0	COMING	14.70	Beginners guide to ice hockey	0
53	0	AVAILABLE	27.80	Being awesome at feeling down	378
54	0	DISCONTINUED	28.40	Learning the basics of designing tree houses	0
55	0	DISCONTINUED	11.00	The secrets of dummies	0
56	0	DISCONTINUED	23.70	The cheap way to meditation	0
57	0	COMING	25.50	Encyclopedia of playing the cello	0
58	0	COMING	27.70	The cheap way to elephants	0
59	0	DISCONTINUED	5.00	Surviving gardening	0
60	0	AVAILABLE	8.60	Becoming one with debugging	6
61	0	COMING	28.60	The life changer: debugging	0
62	0	DISCONTINUED	16.50	Becoming one with intergalaxy travel	0
63	0	COMING	13.70	Encyclopedia of winter bathing	0
64	0	COMING	29.10	The mother of all references: winter bathing	0
65	0	COMING	11.00	The secrets of debugging	0
66	0	AVAILABLE	12.50	The complete visual guide to speaking to a big audience	27
67	0	AVAILABLE	14.40	The life changer: running barefoot	358
68	0	AVAILABLE	9.70	The life changer: keeping your wife happy	168
69	0	AVAILABLE	29.50	The Vaadin way: playing the cello	235
70	0	AVAILABLE	16.60	The secrets of designing tree houses	187
71	0	DISCONTINUED	7.10	Book of ice hockey	0
72	0	AVAILABLE	6.60	Becoming one with keeping your wife happy	102
73	0	AVAILABLE	14.00	The ultimate guide to children's education	509
74	0	DISCONTINUED	12.30	Becoming one with intergalaxy travel	0
75	0	DISCONTINUED	15.70	Book of playing the cello	0
76	0	COMING	19.10	The secrets of designing tree houses	0
77	0	COMING	8.50	Encyclopedia of intergalaxy travel	0
78	0	COMING	25.80	Surviving giant needles	0
79	0	COMING	7.80	The Vaadin way: Vaadin TreeTable	0
80	0	DISCONTINUED	8.80	The complete visual guide to creating software	0
81	0	COMING	6.60	Very much meditation	0
82	0	COMING	28.20	Encyclopedia of rubber bands	0
83	0	COMING	13.60	Mastering speaking to a big audience	0
84	0	DISCONTINUED	10.00	Book of dummies	0
85	0	AVAILABLE	20.00	For fun and profit:  elephants	50
86	0	AVAILABLE	27.00	How to fail at elephants	261
87	0	DISCONTINUED	24.10	The art of computer programming	0
88	0	COMING	26.20	Very much feeling down	0
89	0	AVAILABLE	14.80	Being awesome at computer programming	293
90	0	AVAILABLE	21.60	Avoiding elephants	232
91	0	DISCONTINUED	20.30	Surviving meditation	0
92	0	DISCONTINUED	20.50	The cheap way to ice hockey	0
93	0	COMING	12.10	Encyclopedia of gardening	0
94	0	COMING	19.10	The art of dummies	0
95	0	AVAILABLE	7.60	How to fail at home security	349
96	0	DISCONTINUED	21.70	The mother of all references: creating software	0
97	0	COMING	29.50	The secrets of children's education	0
98	0	DISCONTINUED	11.60	The cheap way to elephants	0
99	0	COMING	8.10	The art of gardening	0
100	0	COMING	19.90	Becoming one with children's education	0
101	0	COMING	28.90	The cheap way to home security	0
102	0	AVAILABLE	22.30	Being awesome at giant needles	339
103	0	COMING	11.20	Book of feeling down	0
104	0	AVAILABLE	24.90	The ultimate guide to Vaadin TreeTable	0
105	0	AVAILABLE	21.10	Very much giant needles	426
106	0	COMING	21.90	Becoming one with speaking to a big audience	0
107	0	DISCONTINUED	22.70	The cheap way to speaking to a big audience	0
108	0	DISCONTINUED	19.40	The cheap way to home security	0
109	0	DISCONTINUED	14.70	Book of running barefoot	0
110	0	DISCONTINUED	8.90	Surviving computer programming	0
111	0	DISCONTINUED	27.30	10 important facts about speaking to a big audience	0
112	0	AVAILABLE	15.40	The ultimate guide to computer programming	468
113	0	COMING	22.30	For fun and profit:  elephants	0
114	0	DISCONTINUED	29.80	Learning the basics of elephants	0
115	0	AVAILABLE	11.20	Surviving computer programming	227
116	0	DISCONTINUED	7.50	The life changer: rubber bands	0
117	0	COMING	22.30	The art of giant needles	0
118	0	AVAILABLE	14.60	The secrets of keeping your wife happy	189
119	0	COMING	19.40	The secrets of home security	0
120	0	AVAILABLE	7.20	The art of designing tree houses	518
121	0	COMING	10.10	10 important facts about Vaadin TreeTable	0
122	0	AVAILABLE	23.30	Avoiding elephants	316
123	0	COMING	15.00	The Vaadin way: giant needles	0
124	0	DISCONTINUED	10.40	10 important facts about designing tree houses	0
125	0	COMING	9.40	The secrets of speaking to a big audience	0
126	0	COMING	5.70	The secrets of creating software	0
127	0	COMING	11.50	Learning the basics of elephants	0
128	0	COMING	15.90	Being awesome at running barefoot	0
129	0	DISCONTINUED	7.00	How to fail at playing the cello	0
130	0	COMING	27.60	The art of meditation	0
131	0	AVAILABLE	10.00	The Vaadin way: children's education	97
132	0	DISCONTINUED	23.10	The ultimate guide to elephants	0
133	0	AVAILABLE	25.60	Encyclopedia of children's education	257
134	0	DISCONTINUED	11.90	The Vaadin way: living a healthy life	0
135	0	DISCONTINUED	9.40	Beginners guide to speaking to a big audience	0
136	0	AVAILABLE	20.30	The ultimate guide to ice hockey	299
137	0	COMING	15.10	The life changer: home security	0
138	0	AVAILABLE	8.00	The cheap way to living a healthy life	338
139	0	DISCONTINUED	9.30	Being awesome at elephants	0
140	0	COMING	12.50	The ultimate guide to rubber bands	0
141	0	COMING	6.30	Surviving playing the cello	0
142	0	DISCONTINUED	14.50	Avoiding rubber bands	0
143	0	DISCONTINUED	12.20	Becoming one with winter bathing	0
144	0	COMING	29.70	Avoiding running barefoot	0
145	0	AVAILABLE	15.80	Learning the basics of playing the cello	72
146	0	DISCONTINUED	7.30	Becoming one with feeling down	0
147	0	AVAILABLE	6.00	Becoming one with elephants	464
148	0	DISCONTINUED	14.80	Avoiding children's education	0
149	0	DISCONTINUED	18.30	10 important facts about intergalaxy travel	0
150	0	COMING	22.60	10 important facts about gardening	0
151	0	DISCONTINUED	5.30	The art of feeling down	0
\.


--
-- Data for Name: product_category; Type: TABLE DATA; Schema: public; Owner: creator
--

COPY public.product_category (product_id, category_id) FROM stdin;
52	1
53	8
54	4
54	8
55	3
56	5
56	6
57	2
57	8
58	7
59	3
60	2
60	3
61	6
62	2
62	8
63	2
63	4
64	1
64	4
65	1
66	3
67	4
68	3
68	8
69	2
70	2
71	1
71	5
72	2
73	6
74	1
74	2
75	3
75	6
76	6
76	7
77	2
78	1
78	6
79	1
80	1
80	5
81	3
82	5
83	6
83	7
84	3
84	7
85	4
86	5
87	1
87	8
88	2
88	6
89	6
89	7
90	1
90	7
91	2
92	5
92	6
93	1
93	6
94	6
95	2
95	3
96	5
97	1
97	3
98	3
99	4
100	7
101	1
101	4
102	6
103	3
104	2
104	7
105	6
106	1
106	3
107	7
108	4
109	3
109	4
110	3
111	3
112	1
113	1
113	2
114	1
114	2
115	1
115	6
116	3
117	3
117	6
118	5
118	6
119	6
120	1
121	2
121	7
122	2
122	5
123	1
123	6
124	1
124	4
125	2
126	7
127	1
127	5
128	4
129	2
130	6
130	8
131	5
131	7
132	3
132	7
133	4
134	6
135	3
135	5
136	1
137	6
138	1
138	6
139	1
139	3
140	4
141	4
142	4
142	6
143	6
144	3
144	6
145	2
146	2
146	5
147	3
148	4
148	7
149	2
150	1
151	5
151	7
\.


--
-- Name: idgenerator; Type: SEQUENCE SET; Schema: public; Owner: creator
--

SELECT pg_catalog.setval('public.idgenerator', 251, true);


--
-- Name: application_user application_user_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.application_user
    ADD CONSTRAINT application_user_pkey PRIMARY KEY (id);


--
-- Name: category category_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.category
    ADD CONSTRAINT category_pkey PRIMARY KEY (id);


--
-- Name: draft_category draft_category_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.draft_category
    ADD CONSTRAINT draft_category_pkey PRIMARY KEY (draft_id, category_id);


--
-- Name: draft draft_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.draft
    ADD CONSTRAINT draft_pkey PRIMARY KEY (id);


--
-- Name: message message_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id);


--
-- Name: product_category product_category_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT product_category_pkey PRIMARY KEY (product_id, category_id);


--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);


--
-- Name: draft fk182ehu5bem243kfbbg9m8mjf3; Type: FK CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.draft
    ADD CONSTRAINT fk182ehu5bem243kfbbg9m8mjf3 FOREIGN KEY (user_id) REFERENCES public.application_user(id);


--
-- Name: draft_category fk4kqo4bt43hu95a1eps91ycsrc; Type: FK CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.draft_category
    ADD CONSTRAINT fk4kqo4bt43hu95a1eps91ycsrc FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- Name: product_category fkja2hwfcn4uqknuehnveejoo0v; Type: FK CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fkja2hwfcn4uqknuehnveejoo0v FOREIGN KEY (product_id) REFERENCES public.product(id);


--
-- Name: draft_category fkjlgxjdy09fd32qlewurq6hbr8; Type: FK CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.draft_category
    ADD CONSTRAINT fkjlgxjdy09fd32qlewurq6hbr8 FOREIGN KEY (draft_id) REFERENCES public.draft(id);


--
-- Name: product_category fkpcmsq096b3sna4u2p9xnxlmgf; Type: FK CONSTRAINT; Schema: public; Owner: creator
--

ALTER TABLE ONLY public.product_category
    ADD CONSTRAINT fkpcmsq096b3sna4u2p9xnxlmgf FOREIGN KEY (category_id) REFERENCES public.category(id);


--
-- PostgreSQL database dump complete
--


// tsd_fb_mux.inc - built-in satellite mux table (v28).
// Fallback for when the TV channel DB has no program/mux rows for a
// satellite triplet: on 2026-11-23 the DB lost all BS (onid 4) and
// CS network 6 program rows and tunes failed with error 9.
// Values: pre-loss DtvData.db MuxInfoTbl (route 8), (onid, tsid) ->
// L-band IF frequency in kHz and satellite id (BS=59, CS=60).
struct tsd_fb_mux { uint16_t onid; uint16_t tsid; uint32_t freq; uint32_t sat_id; };
static const struct tsd_fb_mux tsd_fb_mux_table[] = {
 {4, 16400, 1049480, 59},
 {4, 16401, 1049480, 59},
 {4, 16402, 1049480, 59},
 {4, 16432, 1087840, 59},
 {4, 16433, 1087840, 59},
 {4, 16528, 1202920, 59},
 {4, 16529, 1202920, 59},
 {4, 16530, 1202920, 59},
 {4, 16592, 1279640, 59},
 {4, 16593, 1279640, 59},
 {4, 16625, 1318000, 59},
 {4, 16626, 1318000, 59},
 {4, 17488, 1126200, 59},
 {4, 17489, 1126200, 59},
 {4, 18098, 1241280, 59},
 {4, 18099, 1241280, 59},
 {4, 18130, 1279640, 59},
 {4, 18224, 1394720, 59},
 {4, 18225, 1394720, 59},
 {4, 18226, 1394720, 59},
 {4, 18227, 1394720, 59},
 {4, 18256, 1433080, 59},
 {4, 18257, 1433080, 59},
 {4, 18258, 1433080, 59},
 {4, 18288, 1471440, 59},
 {4, 18801, 1471440, 59},
 {4, 18802, 1471440, 59},
 {4, 18803, 1471440, 59},
 {6, 24608, 1613000, 60},
 {6, 24704, 1733000, 60},
 {6, 24736, 1773000, 60},
 {7, 28736, 1653000, 60},
 {7, 28768, 1693000, 60},
 {7, 28864, 1813000, 60},
 {7, 28896, 1853000, 60},
 {7, 28928, 1893000, 60},
 {7, 28960, 1933000, 60},
 {7, 28992, 1973000, 60},
 {7, 29024, 2013000, 60},
 {7, 29056, 2053000, 60},
};
static const struct tsd_fb_mux *tsd_fb_lookup(uint16_t onid, uint16_t tsid) {
 for(unsigned i=0;i<sizeof(tsd_fb_mux_table)/sizeof(tsd_fb_mux_table[0]);i++)
  if(tsd_fb_mux_table[i].onid==onid&&tsd_fb_mux_table[i].tsid==tsid)return &tsd_fb_mux_table[i];
 return 0;
}

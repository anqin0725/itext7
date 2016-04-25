/*
 * $Id$
 *
 * This file is part of the iText (R) project.
 * Copyright (c) 2014-2015 iText Group NV
 * Authors: Bruno Lowagie, Paulo Soares, et al.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA, 02110-1301 USA, or download the license from the following URL:
 * http://itextpdf.com/terms-of-use/
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License,
 * a covered work must retain the producer line in every PDF that is created
 * or manipulated using iText.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the iText software without
 * disclosing the source code of your own applications.
 * These activities include: offering paid services to customers as an ASP,
 * serving PDFs on the fly in a web application, shipping iText with a closed
 * source product.
 *
 * For more information, please contact iText Software Corp. at this
 * address: sales@itextpdf.com
 *
 *
 * This class is based on the C# open source freeware library Clipper:
 * http://www.angusj.com/delphi/clipper.php
 * The original classes were distributed under the Boost Software License:
 *
 * Freeware for both open source and commercial applications
 * Copyright 2010-2014 Angus Johnson
 * Boost Software License - Version 1.0 - August 17th, 2003
 *
 * Permission is hereby granted, free of charge, to any person or organization
 * obtaining a copy of the software and accompanying documentation covered by
 * this license (the "Software") to use, reproduce, display, distribute,
 * execute, and transmit the Software, and to prepare derivative works of the
 * Software, and to permit third-parties to whom the Software is furnished to
 * do so, all subject to the following:
 *
 * The copyright notices in the Software and this entire statement, including
 * the above license grant, this restriction and the following disclaimer,
 * must be included in all copies of the Software, in whole or in part, and
 * all derivative works of the Software, unless such copies or derivative
 * works are solely in the form of machine-executable object code generated by
 * a source language processor.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
 * FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.itextpdf.kernel.pdf.canvas.parser.clipper;

import com.itextpdf.kernel.pdf.canvas.parser.clipper.IClipper.ClipType;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.IClipper.Direction;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.IClipper.PolyFillType;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.IClipper.PolyType;
import com.itextpdf.kernel.pdf.canvas.parser.clipper.Point.LongPoint;

import java.math.BigInteger;
import java.util.logging.Logger;

class Edge {
    static enum Side {
        LEFT, RIGHT
    }

    static boolean doesE2InsertBeforeE1( Edge e1, Edge e2 ) {
        if (e2.current.getX() == e1.current.getX()) {
            if (e2.top.getY() > e1.top.getY()) {
                return e2.top.getX() < topX( e1, e2.top.getY() );
            }
            else {
                return e1.top.getX() > topX( e2, e1.top.getY() );
            }
        }
        else {
            return e2.current.getX() < e1.current.getX();
        }
    }

    static boolean slopesEqual( Edge e1, Edge e2, boolean useFullRange ) {
        if (useFullRange) {
            return BigInteger.valueOf(e1.getDelta().getY()).multiply(BigInteger.valueOf(e2.getDelta().getX())).equals(
                   BigInteger.valueOf(e1.getDelta().getX()).multiply(BigInteger.valueOf(e2.getDelta().getY())));
        } else {
            return (e1.getDelta().getY()) * (e2.getDelta().getX()) == (e1.getDelta().getX()) * (e2.getDelta().getY());
        }
    }

    static void swapPolyIndexes( Edge edge1, Edge edge2 ) {
        final int outIdx = edge1.outIdx;
        edge1.outIdx = edge2.outIdx;
        edge2.outIdx = outIdx;
    }

    static void swapSides( Edge edge1, Edge edge2 ) {
        final Edge.Side side = edge1.side;
        edge1.side = edge2.side;
        edge2.side = side;
    }

    static long topX( Edge edge, long currentY ) {
        if (currentY == edge.getTop().getY()) {
            return edge.getTop().getX();
        }
        return (edge.getBot().getX() + Math.round(edge.deltaX * (currentY - edge.getBot().getY())));
    }

    private final LongPoint bot;

    private final LongPoint current;

    private final LongPoint top;

    private final LongPoint delta;
    double deltaX;

    PolyType polyTyp;

    Edge.Side side;

    int windDelta; //1 or -1 depending on winding direction

    int windCnt;
    int windCnt2; //winding count of the opposite polytype
    int outIdx;
    Edge next;
    Edge prev;
    Edge nextInLML;
    Edge nextInAEL;
    Edge prevInAEL;
    Edge nextInSEL;
    Edge prevInSEL;

    protected static final int SKIP = -2;

    protected static final int UNASSIGNED = -1;

    protected static final double HORIZONTAL = -3.4E+38;

    private static final Logger LOGGER = Logger.getLogger(Edge.class.getName());

    public Edge() {
        delta = new LongPoint();
        top = new LongPoint();
        bot = new LongPoint();
        current = new LongPoint();
    }

    public Edge findNextLocMin() {
        Edge e = this;
        Edge e2;
        for (;;) {
            while (!e.bot.equals( e.prev.bot ) || e.current.equals( e.top )) {
                e = e.next;
            }
            if (e.deltaX != Edge.HORIZONTAL && e.prev.deltaX != Edge.HORIZONTAL) {
                break;
            }
            while (e.prev.deltaX == Edge.HORIZONTAL) {
                e = e.prev;
            }
            e2 = e;
            while (e.deltaX == Edge.HORIZONTAL) {
                e = e.next;
            }
            if (e.top.getY() == e.prev.bot.getY()) {
                continue; //ie just an intermediate horz.
            }
            if (e2.prev.bot.getX() < e.bot.getX()) {
                e = e2;
            }
            break;
        }
        return e;
    }

    public LongPoint getBot() {
        return bot;
    }

    public LongPoint getCurrent() {
        return current;
    }

    public LongPoint getDelta() {
        return delta;
    }

    public Edge getMaximaPair() {
        Edge result = null;
        if (next.top.equals( top ) && next.nextInLML == null) {
            result = next;
        }
        else if (prev.top.equals( top ) && prev.nextInLML == null) {
            result = prev;
        }
        if (result != null && (result.outIdx == Edge.SKIP || result.nextInAEL == result.prevInAEL && !result.isHorizontal())) {
            return null;
        }
        return result;
    }

    public Edge getNextInAEL( Direction direction ) {
        return direction == Direction.LEFT_TO_RIGHT ? nextInAEL : prevInAEL;
    }

    public LongPoint getTop() {
        return top;
    }

    public boolean isContributing( PolyFillType clipFillType, PolyFillType subjFillType, ClipType clipType ) {
        LOGGER.entering( Edge.class.getName(), "isContributing" );

        PolyFillType pft, pft2;
        if (polyTyp == PolyType.SUBJECT) {
            pft = subjFillType;
            pft2 = clipFillType;
        }
        else {
            pft = clipFillType;
            pft2 = subjFillType;
        }

        switch (pft) {
            case EVEN_ODD:
                //return false if a subj line has been flagged as inside a subj polygon
                if (windDelta == 0 && windCnt != 1) {
                    return false;
                }
                break;
            case NON_ZERO:
                if (Math.abs(windCnt) != 1) {
                    return false;
                }
                break;
            case POSITIVE:
                if (windCnt != 1) {
                    return false;
                }
                break;
            default: //PolyFillType.pftNegative
                if (windCnt != -1) {
                    return false;
                }
                break;
        }

        switch (clipType) {
            case INTERSECTION:
                switch (pft2) {
                    case EVEN_ODD:
                    case NON_ZERO:
                        return windCnt2 != 0;
                    case POSITIVE:
                        return windCnt2 > 0;
                    default:
                        return windCnt2 < 0;
                }
            case UNION:
                switch (pft2) {
                    case EVEN_ODD:
                    case NON_ZERO:
                        return windCnt2 == 0;
                    case POSITIVE:
                        return windCnt2 <= 0;
                    default:
                        return windCnt2 >= 0;
                }
            case DIFFERENCE:
                if (polyTyp == PolyType.SUBJECT) {
                    switch (pft2) {
                        case EVEN_ODD:
                        case NON_ZERO:
                            return windCnt2 == 0;
                        case POSITIVE:
                            return windCnt2 <= 0;
                        default:
                            return windCnt2 >= 0;
                    }
                }
                else {
                    switch (pft2) {
                        case EVEN_ODD:
                        case NON_ZERO:
                            return windCnt2 != 0;
                        case POSITIVE:
                            return windCnt2 > 0;
                        default:
                            return windCnt2 < 0;
                    }
                }
            case XOR:
                if (windDelta == 0) {
                    switch (pft2) {
                        case EVEN_ODD:
                        case NON_ZERO:
                            return windCnt2 == 0;
                        case POSITIVE:
                            return windCnt2 <= 0;
                        default:
                            return windCnt2 >= 0;
                    }
                }
                else {
                    return true;
                }
        }
        return true;
    }

    public boolean isEvenOddAltFillType( PolyFillType clipFillType, PolyFillType subjFillType ) {
        if (polyTyp == PolyType.SUBJECT) {
            return clipFillType == PolyFillType.EVEN_ODD;
        }
        else {
            return subjFillType == PolyFillType.EVEN_ODD;
        }
    }

    public boolean isEvenOddFillType( PolyFillType clipFillType, PolyFillType subjFillType ) {
        if (polyTyp == PolyType.SUBJECT) {
            return subjFillType == PolyFillType.EVEN_ODD;
        }
        else {
            return clipFillType == PolyFillType.EVEN_ODD;
        }
    }

    public boolean isHorizontal() {
        return delta.getY() == 0;
    }

    public boolean isIntermediate( double y ) {
        return top.getY() == y && nextInLML != null;
    }

    public boolean isMaxima( double Y ) {
        return top.getY() == Y && nextInLML == null;
    }

    public void reverseHorizontal() {
        //swap horizontal edges' top and bottom x's so they follow the natural
        //progression of the bounds - ie so their xbots will align with the
        //adjoining lower edge. [Helpful in the ProcessHorizontal() method.]
        long temp = top.getX();
        top.setX( bot.getX() );
        bot.setX( temp );

        temp = top.getZ();
        top.setZ( bot.getZ() );
        bot.setZ( temp );

    }

    public void setBot( LongPoint bot ) {
        this.bot.set( bot );
    }

    public void setCurrent( LongPoint current ) {
        this.current.set( current );
    }

    public void setTop( LongPoint top ) {
        this.top.set( top );
    }

    @Override
    public String toString() {
        return "TEdge [Bot=" + bot + ", Curr=" + current + ", Top=" + top + ", Delta=" + delta + ", Dx=" + deltaX + ", PolyTyp=" + polyTyp + ", Side=" + side
                        + ", WindDelta=" + windDelta + ", WindCnt=" + windCnt + ", WindCnt2=" + windCnt2 + ", OutIdx=" + outIdx + ", Next=" + next + ", Prev="
                        + prev + ", NextInLML=" + nextInLML + ", NextInAEL=" + nextInAEL + ", PrevInAEL=" + prevInAEL + ", NextInSEL=" + nextInSEL
                        + ", PrevInSEL=" + prevInSEL + "]";
    }

    public void updateDeltaX() {

        delta.setX( top.getX() - bot.getX() );
        delta.setY( top.getY() - bot.getY() );
        if (delta.getY() == 0) {
            deltaX = Edge.HORIZONTAL;
        }
        else {
            deltaX = (double) delta.getX() / delta.getY();
        }
    }

};
